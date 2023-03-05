package net.mt32.expoll

typealias tUserID = String
typealias tPollID = String
typealias tOptionID = Int


enum class PollType(val id: Int){
    STRING(0),
    DATE(1),
    DATETIME(2)
}

enum class VoteValue(val id: Int){
    NO(0),
    YES(1),
    MAYBE(2)
}