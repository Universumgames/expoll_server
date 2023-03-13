package net.mt32.expoll.database.transform

import net.mt32.expoll.database.Transformer
import net.mt32.expoll.entities.Poll
import net.mt32.expoll.entities.Vote

/**
 * Remove votes for options that do not exist anymore
 */
fun Transformer.removeGhostVotes(): Boolean {
    val votes = Vote.all()
    val polls = Poll.all()

    votes.forEach { vote ->
        val poll = polls.find{
            it.id == vote.pollID
        }
        // remove all votes which poll does not exist anymore
        if(poll == null){
            vote.delete()
            return@forEach
        }
        // remove votes where their options does not exist anymore
        if(!poll.options.map { it.id }.contains(vote.optionID)){
            vote.delete()
            return@forEach
        }
    }
    return true
}