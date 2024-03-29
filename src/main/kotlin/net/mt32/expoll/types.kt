package net.mt32.expoll

typealias tUserID = String
typealias tPollID = String
typealias tOptionID = Int

typealias tClientDateTime = Long
typealias tClientDate = Long


enum class PollType(val id: Int){
    STRING(0),
    DATE(1),
    DATETIME(2);

    companion object{
        fun valueOf(value: Int): PollType{
            return values().find { it.id == value } ?: STRING
        }
    }
}

enum class VoteValue(val id: Int){
    UNKNOWN(-1),
    NO(0),
    YES(1),
    MAYBE(2);

    val translationKey: String
        get() = when(this){
            UNKNOWN -> "vote.unknown"
            NO -> "vote.no"
            YES -> "vote.yes"
            MAYBE -> "vote.maybe"
        }

    companion object{
        fun valueOf(value: Int?): VoteValue? {
            return entries.find { it.id == value }
        }
    }
}