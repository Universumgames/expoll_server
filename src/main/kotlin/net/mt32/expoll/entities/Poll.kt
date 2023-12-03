package net.mt32.expoll.entities

import kotlinx.serialization.Serializable
import net.mt32.expoll.PollType
import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.*
import net.mt32.expoll.serializable.request.SortingOrder
import net.mt32.expoll.serializable.responses.*
import net.mt32.expoll.tPollID
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.reflect.full.memberProperties

interface IPoll {
    val admin: User
    val id: String
    var name: String
    val createdTimestamp: UnixTimestamp
    var updatedTimestamp: UnixTimestamp
    var description: String
    val type: PollType
    var maxPerUserVoteCount: Int
    var allowsMaybe: Boolean
    var allowsEditing: Boolean
}

class Poll : DatabaseEntity, IPoll {

    override val admin: User
        get() {
            return User.loadFromID(adminID)!!
        }
    val adminID: tUserID
    override val id: String
    override var name: String
    override val createdTimestamp: UnixTimestamp
    override var updatedTimestamp: UnixTimestamp
    override var description: String
    override val type: PollType
    val votes: List<Vote>
        get() {
            return Vote.fromPoll(id).filter { vote -> users.map { it.id }.contains(vote.userID) }
        }
    val notes: List<PollUserNote>
        get() {
            return PollUserNote.forPoll(id)
        }
    val users: List<User>
        get() {
            return usersInPoll(id)
        }
    val userCount: Int
        get() = UserPolls.userCount(id)

    val userIDs: List<tUserID>
        get() = UserPolls.userIDs(id)

    val joinedTimestamps: List<PollJoinTimestamp>
        get() = UserPolls.joinedTimestamps(id)

    override var maxPerUserVoteCount: Int
    override var allowsMaybe: Boolean
    override var allowsEditing: Boolean

    val shareURL: String
        get() = URLBuilder.shareURLBuilder(id)

    val options: List<PollOption>
        get() {
            return when (type) {
                PollType.STRING -> PollOptionString.fromPollID(id)
                PollType.DATE -> PollOptionDate.fromPollID(id)
                PollType.DATETIME -> PollOptionDateTime.fromPollID(id)
            }
        }

    constructor(
        adminID: tUserID,
        id: String,
        name: String,
        createdTimestamp: UnixTimestamp,
        updatedTimestamp: UnixTimestamp,
        description: String,
        type: PollType,
        maxPerUserVoteCount: Int,
        allowsMaybe: Boolean,
        allowsEditing: Boolean
    ) : super() {
        this.adminID = adminID
        this.id = id
        this.name = name
        this.createdTimestamp = createdTimestamp
        this.updatedTimestamp = updatedTimestamp
        this.description = description
        this.type = type
        this.maxPerUserVoteCount = maxPerUserVoteCount
        this.allowsMaybe = allowsMaybe
        this.allowsEditing = allowsEditing
    }


    private constructor(pollRow: ResultRow) {
        this.id = pollRow[Poll.id]
        this.adminID = pollRow[Poll.adminID]
        this.name = pollRow[Poll.name]
        this.description = pollRow[Poll.description]
        this.type = PollType.valueOf(pollRow[Poll.type])
        this.createdTimestamp = pollRow[Poll.createdTimestamp].toUnixTimestampFromDB()
        this.updatedTimestamp = pollRow[Poll.updatedTimestamp].toUnixTimestampFromDB()
        this.maxPerUserVoteCount = pollRow[Poll.maxPerUserVoteCount]
        this.allowsMaybe = pollRow[Poll.allowsMaybe]
        this.allowsEditing = pollRow[Poll.allowsEditing]
    }

    override fun save(): Boolean {
        updatedTimestamp = UnixTimestamp.now()
        transaction {
            options.forEach { option ->
                option.save()
            }
            notes.forEach { note ->
                note.save()
            }
            votes.forEach { vote ->
                vote.save()
            }
            transaction {
                Poll.upsertCustom(Poll.id) {
                    it[Poll.id] = this@Poll.id
                    it[Poll.adminID] = this@Poll.adminID
                    it[Poll.name] = this@Poll.name
                    it[Poll.description] = this@Poll.description.replace("\\n", "\n").replace("\\t", "\t")
                    it[Poll.type] = this@Poll.type.id
                    it[Poll.createdTimestamp] = this@Poll.createdTimestamp.toDB()
                    it[Poll.updatedTimestamp] = this@Poll.updatedTimestamp.toDB()
                    it[Poll.maxPerUserVoteCount] = this@Poll.maxPerUserVoteCount
                    it[Poll.allowsMaybe] = this@Poll.allowsMaybe
                    it[Poll.allowsEditing] = this@Poll.allowsEditing
                }
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            UserPolls.deleteWhere { UserPolls.pollID eq this@Poll.id }
            options.forEach { option -> option.delete() }
            notes.forEach { note -> note.delete() }
            votes.forEach { vote -> vote.delete() }

            Poll.deleteWhere {
                Poll.id eq this@Poll.id
            }
        }
        return true
    }

    companion object : Table("poll") {
        const val MAX_DESCRIPTION_LENGTH = 65535

        val id = varchar("id", UUIDLength)
        val adminID = varchar("adminId", UUIDLength)
        val name = varchar("name", 255)
        val createdTimestamp = long("createdTimestamp")
        val updatedTimestamp = long("updatedTimestamp")
        val description = varchar("description", MAX_DESCRIPTION_LENGTH)
        val type = integer("type")
        val maxPerUserVoteCount = integer("maxPerUserVoteCount")
        val allowsMaybe = bool("allowsMaybe")
        val allowsEditing = bool("allowsEditing")

        override val primaryKey = PrimaryKey(id)

        fun fromID(pollID: tPollID): Poll? {
            return transaction {
                val pollRow = Poll.select { Poll.id eq pollID }.firstOrNull()
                return@transaction pollRow?.let { Poll(it) }
            }
        }

        fun accessibleForUser(userID: tUserID): List<Poll> {
            return transaction {
                val pollIDs = UserPolls.select { UserPolls.userID eq userID }.map { it[UserPolls.pollID] }
                return@transaction pollIDs.map { Poll.fromID(it) }.filterNotNull()
            }
        }

        fun usersInPoll(pollID: tPollID): List<User> {
            return transaction {
                val userIDs = UserPolls.select { UserPolls.pollID eq pollID }.map { it[UserPolls.userID] }
                return@transaction userIDs.map { User.loadFromID(it) }.filterNotNull()
            }
        }

        fun setUsersInPoll(pollID: tPollID, userIDs: List<tUserID>) {
            transaction {
                UserPolls.deleteWhere { UserPolls.pollID eq pollID }
                UserPolls.batchInsert(userIDs) { userID ->
                    this[UserPolls.userID] = userID
                    this[UserPolls.pollID] = pollID
                }
            }
        }

        fun newID(): tPollID {
            return transaction {
                var id: String
                do {
                    id = UUID.randomUUID().toString()
                } while (fromID(id) != null)
                return@transaction id
            }
        }

        fun createPoll(
            adminID: tUserID,
            name: String,
            description: String,
            type: PollType,
            maxPerUserVoteCount: Int,
            allowsMaybe: Boolean,
            allowsEditing: Boolean
        ): Poll {
            return Poll(
                adminID,
                newID(),
                name,
                UnixTimestamp.now(),
                UnixTimestamp.now(),
                description,
                type,
                maxPerUserVoteCount,
                allowsMaybe,
                allowsEditing
            )
        }

        fun all(): List<Poll> {
            return transaction {
                return@transaction Poll.selectAll().toList().map { Poll(it) }
            }
        }

        fun all(
            limit: Int = -1,
            offset: Long = 0,
            searchParameters: PollSearchParameters? = null,
            forUserId: tUserID? = null
        ): List<Poll> {
            return transaction {
                val query = if (searchParameters == null) Poll.selectAll()
                else Poll.select {
                    val specialFilter = when (searchParameters.specialFilter) {
                        PollSearchParameters.SpecialFilter.ALL -> Op.TRUE
                        PollSearchParameters.SpecialFilter.JOINED -> forUserId?.let {
                            Poll.id inSubQuery UserPolls.select { UserPolls.userID eq forUserId }
                                .adjustSlice { slice(Poll.id) }
                        } ?: Op.TRUE

                        PollSearchParameters.SpecialFilter.NOT_JOINED -> forUserId?.let {
                            Poll.id notInSubQuery UserPolls.select { UserPolls.userID eq forUserId }
                                .adjustSlice { slice(Poll.id) }
                        } ?: Op.TRUE
                    }

                    val adminID =
                        (if (searchParameters.searchQuery.adminID != null) (Poll.adminID like "%${searchParameters.searchQuery.adminID}%") else Op.TRUE)
                    val name =
                        (if (searchParameters.searchQuery.name != null) (Poll.name like "%${searchParameters.searchQuery.name}%") else Op.TRUE)
                    val description =
                        (if (searchParameters.searchQuery.description != null) (Poll.description like "%${searchParameters.searchQuery.description}%") else Op.TRUE)
                    val userPolls =
                        UserPolls.select { UserPolls.userID like "%${searchParameters.searchQuery.memberID}%" }
                            .adjustSlice { slice(UserPolls.pollID) }
                    val memberID =
                        (if (searchParameters.searchQuery.memberID != null) (Poll.id inSubQuery userPolls) else Op.TRUE)
                    val any = (if (searchParameters.searchQuery.any != null) (
                            (Poll.name eq "%${searchParameters.searchQuery.any}%") or
                                    (Poll.id like "%${searchParameters.searchQuery.any}%") or
                                    (Poll.description like "%${searchParameters.searchQuery.any}%") or memberID) else Op.TRUE)

                    val query = adminID and name and description and memberID and any

                    return@select query and specialFilter
                }
                val sorted = query.orderBy(
                    when (searchParameters?.sortingStrategy) {
                        PollSearchParameters.SortingStrategy.CREATED -> Poll.createdTimestamp
                        PollSearchParameters.SortingStrategy.UPDATED -> Poll.updatedTimestamp
                        PollSearchParameters.SortingStrategy.NAME -> Poll.name
                        PollSearchParameters.SortingStrategy.USER_COUNT -> Poll.id
                        else -> Poll.updatedTimestamp
                    } to when (searchParameters?.sortingOrder) {
                        SortingOrder.ASCENDING -> SortOrder.ASC
                        SortingOrder.DESCENDING -> SortOrder.DESC
                        null -> SortOrder.ASC
                    }
                )
                val limited = if (limit > 0) sorted.limit(limit, offset) else sorted
                return@transaction limited.toList().map { Poll(it) }
            }
        }

        fun exists(pollID: tPollID): Boolean {
            return transaction {
                !Poll.select { Poll.id eq pollID }.empty()
            }
        }

        fun ownedByUser(userID: tUserID): List<Poll> {
            return transaction {
                return@transaction Poll.select {(Poll.adminID eq userID) }.map { Poll(it) }
            }
        }

        fun ownedByUserCount(userID: tUserID): Long {
            return transaction {
                return@transaction Poll.select {(Poll.adminID eq userID) }.count()
            }
        }
    }

    fun asDetailedPoll(): DetailedPollResponse {
        val userIDs = this.userIDs
        val users = this.users
        val options = this.options
        val notes = this.notes
        val joinTimestamps = this.joinedTimestamps

        val relevantOptionID = if (type == PollType.STRING) null else {
            val optionIndex = options.indexOfFirst { option ->
                when (type) {
                    PollType.DATE -> (option as PollOptionDate).dateStartTimestamp.addDays(1) > UnixTimestamp.now()
                    PollType.DATETIME -> (option as PollOptionDateTime).dateTimeStartTimestamp > UnixTimestamp.now()
                    else -> false
                }
            }
            if (optionIndex == -1) null else options[optionIndex].id
        }
        return DetailedPollResponse(id,
            name,
            admin.asSimpleUser(),
            description,
            maxPerUserVoteCount,
            userCount,
            updatedTimestamp.toClient(),
            createdTimestamp.toClient(),
            type.id,
            options.map {
                val opt = it.toComplexOption()
                opt.isMostRelevant = it.id == relevantOptionID
                opt
            },
            users.map { user ->
                val votes = Vote.fromUserPoll(user.id, id)//.filter { options.map { it.id }.contains(it.optionID) }
                val existingVotesOptionIds = votes.map { it.optionID }
                val missingVotes = options.map { it.id }.filterNot { existingVotesOptionIds.contains(it) }
                UserVote(PollSimpleUser(
                    user.firstName,
                    user.lastName,
                    user.username,
                    user.id,
                    joinTimestamps.find { it.userID == user.id }!!.joinTimestamp.toClient()
                ), votes.map { note -> SimpleVote(note.optionID, note.votedFor.id) } +
                        // add null votes for non existing votes on options
                        missingVotes.map {
                            SimpleVote(
                                it, null
                            )
                        })
            },
            userIDs.map { userID ->
                val note = notes.find { note -> note.userID == userID }
                UserNote(userID, note?.note)
            }.filterNot { it.note == null },
            allowsMaybe,
            allowsEditing,
            shareURL,
            UserPolls.getHidden(id, adminID)
        )
    }

    fun asSimplePoll(user: User?): PollSummary {
        return PollSummary(id,
            name,
            admin.asSimpleUser(),
            description,
            userCount,
            updatedTimestamp.toClient(),
            type.id,
            allowsEditing,
            user?.let { UserPolls.getHidden(id, user.id) } ?: false)
    }

    /**
     * adds a new option, if type of complex option does not match the one of the poll, nothing changes
     * @return true when types match, false otherwise
     */
    fun addOption(option: ComplexOption): Boolean {
        when (type) {
            PollType.STRING -> {
                if (option.value == null) return false
                PollOptionString(option.value, id, PollOptionString.newID(id)).save()
                return true
            }

            PollType.DATE -> {
                if (option.dateStart == null) return false
                PollOptionDate(
                    option.dateStart.toUnixTimestampFromClient(),
                    option.dateEnd?.toUnixTimestampFromClient(),
                    id,
                    PollOptionDate.newID(id)
                ).save()
                return true
            }

            PollType.DATETIME -> {
                if (option.dateTimeStart == null) return false
                PollOptionDateTime(
                    option.dateTimeStart.toUnixTimestampFromClient(),
                    option.dateTimeEnd?.toUnixTimestampFromClient(),
                    id,
                    PollOptionDateTime.newID(id)
                ).save()
                return true
            }
        }
    }

    fun addUser(userID: tUserID) {
        UserPolls.addConnection(userID, id)
        updatedTimestamp = UnixTimestamp.now()
    }

    fun removeUser(userID: tUserID) {
        UserPolls.removeConnection(userID, id)
        transaction {
            Vote.deleteWhere {
                (Vote.pollID eq pollID) and (Vote.userID eq userID)
            }
        }
    }

}

@Serializable
data class PollSearchParameters(
    var sortingOrder: SortingOrder = SortingOrder.DESCENDING,
    var sortingStrategy: SortingStrategy = SortingStrategy.UPDATED,
    var specialFilter: SpecialFilter = SpecialFilter.ALL,
    var searchQuery: Query = Query()
) {
    enum class SortingStrategy {
        UPDATED, CREATED, NAME, USER_COUNT
    }

    enum class SpecialFilter {
        ALL, JOINED, NOT_JOINED
    }

    @Serializable
    data class Query(
        val adminID: String? = null,
        val description: String? = null,
        val name: String? = null,
        val memberID: String? = null,
        val any: String? = null
    )

    @Serializable
    data class Descriptor(
        val sortingOrder: List<SortingOrder> = SortingOrder.values().toList(),
        val sortingStrategy: List<SortingStrategy> = SortingStrategy.values().toList(),
        val specialFilter: List<SpecialFilter> = SpecialFilter.values().toList(),
        val searchQuery: List<String> = Query::class.memberProperties.map { it.name }
    )
}

data class PollJoinTimestamp(val userID: tUserID, val joinTimestamp: UnixTimestamp) {

}

object UserPolls : Table("user_polls_poll") {
    val userID = varchar("userId", UUIDLength)
    val pollID = varchar("pollId", UUIDLength)
    val joinedTimestamp = long("joinedTimestamp")
    val listHidden = bool("listHidden")

    override val primaryKey = PrimaryKey(userID, pollID)

    fun connectionExists(userID: tUserID, pollID: tPollID): Boolean {
        return transaction {
            return@transaction !UserPolls.select {
                (UserPolls.userID eq userID) and (UserPolls.pollID eq pollID)
            }.empty()
        }
    }

    fun addConnection(
        userID: tUserID, pollID: tPollID, joinTimestamp: UnixTimestamp = UnixTimestamp.now(), hide: Boolean = false
    ) {
        if (connectionExists(userID, pollID)) return
        transaction {
            UserPolls.insert {
                it[UserPolls.pollID] = pollID
                it[UserPolls.userID] = userID
                it[UserPolls.joinedTimestamp] = joinTimestamp.toDB()
                it[UserPolls.listHidden] = hide
            }
        }
    }

    fun removeConnection(userID: tUserID, pollID: tPollID) {
        transaction {
            UserPolls.deleteWhere {
                (UserPolls.userID eq userID) and (UserPolls.pollID eq pollID)
            }
        }
    }

    fun userIDs(pollID: tPollID): List<tUserID> {
        return transaction {
            return@transaction UserPolls.select {
                UserPolls.pollID eq pollID
            }.toList().map { it[UserPolls.userID] }
        }
    }

    fun userCount(pollID: tPollID): Int {
        return userIDs(pollID).size
    }

    fun joinedTimestamps(pollID: tPollID): List<PollJoinTimestamp> {
        return transaction {
            return@transaction UserPolls.select {
                UserPolls.pollID eq pollID
            }.toList()
                .map { PollJoinTimestamp(it[UserPolls.userID], it[UserPolls.joinedTimestamp].toUnixTimestampFromDB()) }
        }
    }

    fun hideFromListForUser(pollID: tPollID, userID: tUserID, hidden: Boolean = true) {
        val joinTimestamp = transaction {
            UserPolls.select { (UserPolls.pollID eq pollID) and (UserPolls.userID eq userID) }.firstOrNull()
                ?.get(UserPolls.joinedTimestamp)
        }?.toUnixTimestampAsSecondsSince1970() ?: UnixTimestamp.now()
        removeConnection(userID, pollID)
        addConnection(userID, pollID, joinTimestamp, hidden)
    }

    fun getHidden(pollID: tPollID, userID: tUserID): Boolean {
        return transaction {
            val rs = UserPolls.select { (UserPolls.pollID eq pollID) and (UserPolls.userID eq userID) }.firstOrNull()
            val hidden = rs?.getOrNull(UserPolls.listHidden)
            return@transaction hidden ?: false
        }
    }
}