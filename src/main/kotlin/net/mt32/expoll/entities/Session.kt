package net.mt32.expoll.entities

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import net.mt32.expoll.auth.JWTSessionPrincipal
import net.mt32.expoll.config
import net.mt32.expoll.database.DatabaseEntity
import net.mt32.expoll.database.UUIDLength
import net.mt32.expoll.helper.*
import net.mt32.expoll.serializable.responses.SafeSession
import net.mt32.expoll.tUserID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.util.Random
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.pow

class OTP : DatabaseEntity {
    val otp: String
    val userID: tUserID
    val expirationTimestamp: UnixTimestamp

    val valid: Boolean
        get() = expirationTimestamp > UnixTimestamp.now()

    constructor(otp: String, userID: tUserID, expirationTimestamp: UnixTimestamp = UnixTimestamp.now().addHours(1)) {
        this.otp = otp
        this.userID = userID
        this.expirationTimestamp = expirationTimestamp
    }

    private constructor(resultRow: ResultRow) {
        this.otp = resultRow[OTP.otp]
        this.userID = resultRow[OTP.userID]
        this.expirationTimestamp = resultRow[OTP.expirationTimestamp].toUnixTimestampFromDB()
    }

    companion object : Table("otp") {
        val otp = varchar("otp", 64)
        val userID = varchar("userid", UUIDLength)
        val expirationTimestamp = long("expirationTimestamp")

        fun fromOTP(otp: String): OTP? {
            if(otp == config.testUser.otp){
                val testuser = User.byUsername(config.testUser.username) ?: return null
                return OTP(otp, testuser.id)
            }
            return transaction {
                val otp = OTP.select { OTP.otp eq otp }.firstOrNull()?.let { OTP(it) }
                if (otp != null && !otp.valid) {
                    otp.delete()
                    return@transaction null
                }
                return@transaction otp
            }
        }

        private fun randomOTP(): String {
            var otp = ""
            do {
                val bytes = ByteArray(16)
                ThreadLocalRandom.current().nextBytes(bytes)
                otp = bytes.toBase64()
            } while (fromOTP(otp) != null)
            return otp
        }

        fun create(userID: tUserID): OTP {
            val otp = OTP(randomOTP(), userID)
            otp.save()
            return otp
        }

        fun fromUser(userID: tUserID): List<OTP>{
            return transaction {
                val result = OTP.select { OTP.userID eq userID }
                return@transaction result.map { OTP(it) }
            }
        }
    }

    override fun save(): Boolean {
        transaction {
            OTP.upsert(OTP.otp) {
                it[otp] = this@OTP.otp
                it[userID] = this@OTP.userID
                it[expirationTimestamp] = this@OTP.expirationTimestamp.toDB()
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            OTP.deleteWhere { otp eq this@OTP.otp }
        }
        return true
    }

    fun createSessionAndDeleteSelf(userAgent: String): Session {
        val session = Session(
            userID,
            userAgent
        )
        session.save()
        delete()
        return session
    }
}

class Session : DatabaseEntity {
    val userID: tUserID
    val nonce: Long
    val userAgent: String
    val createdTimestamp: UnixTimestamp
    val expirationTimestamp: UnixTimestamp
    var lastUsedTimestamp: UnixTimestamp

    val user: User?
        get() = User.loadFromID(userID)

    constructor(
        userID: tUserID,
        userAgent: String
    ) : super() {
        this.userID = userID
        this.nonce = newNonce()
        this.userAgent = userAgent
        this.createdTimestamp = UnixTimestamp.now()
        this.expirationTimestamp = UnixTimestamp.now().addDays(120)
        this.lastUsedTimestamp = UnixTimestamp.now()
    }

    constructor(
        userID: tUserID,
        nonce: Long,
        userAgent: String,
        createdTimestamp: UnixTimestamp,
        expirationTimestamp: UnixTimestamp,
        lastUsedTimestamp: UnixTimestamp
    ) : super() {
        this.userID = userID
        this.nonce = nonce
        this.userAgent = userAgent
        this.createdTimestamp = createdTimestamp
        this.expirationTimestamp = expirationTimestamp
        this.lastUsedTimestamp = lastUsedTimestamp
    }

    constructor(resultRow: ResultRow) {
        this.userID = resultRow[Session.userID]
        this.nonce = resultRow[Session.nonce]
        this.userAgent = resultRow[Session.userAgent]
        this.createdTimestamp = resultRow[Session.createdTimestamp].toUnixTimestampFromDB()
        this.expirationTimestamp = resultRow[Session.expirationTimestamp].toUnixTimestampFromDB()
        this.lastUsedTimestamp = resultRow[Session.lastUsedTimestamp].toUnixTimestampFromDB()
    }


    fun getJWT(): String {
        val user = this.user!!
        return JWT.create()
            .withAudience(config.jwt.audience)

            .withClaim("userID", user.id)
            .withClaim("username", user.username)
            .withClaim("mail", user.mail)
            .withClaim("firstName", user.firstName)
            .withClaim("lastName", user.lastName)
            .withClaim("admin", user.admin)
            .withClaim("superAdmin", user.superAdmin)

            .withClaim("nonce", nonce)
            .withIssuedAt(createdTimestamp.toDate())
            .withExpiresAt(expirationTimestamp.toDate())
            .withIssuer(config.jwt.issuer)
            .withAudience(config.jwt.audience)
            .sign(Algorithm.HMAC256(config.jwt.secret))
    }


    companion object : Table("jwtSession") {
        val userID = varchar("userID", UUIDLength)
        val nonce = long("nonce")
        val userAgent = varchar("userAgent", 500)
        val createdTimestamp = long("createdTimestamp")
        val expirationTimestamp = long("expirationTimestamp")
        val lastUsedTimestamp = long("lastUsedTimestamp")

        fun newNonce(): Long {
            var nonce: Long
            val maxValue = Long.MAX_VALUE
            val minValue = 10.0.pow((maxValue.toString().length - 1).toDouble()).toLong()

            val random = Random()
            do {
                nonce = (minValue + (random.nextDouble() * (maxValue - minValue))).toLong()
            } while (fromNonce(nonce) != null)
            return nonce
        }

        fun forUser(userID: tUserID): List<Session> {
            return transaction {
                return@transaction Session.select { Session.userID eq userID }.map { Session(it) }
            }
        }

        fun fromNonce(nonce: Long): Session? {
            return transaction {
                return@transaction Session.select { Session.nonce eq nonce }.firstOrNull()?.let { Session(it) }
            }
        }

        suspend fun loadAndVerify(call: ApplicationCall, credential: JWTCredential, withAdmin: Boolean = false): JWTSessionPrincipal? {
            if (credential.expiresAt != null && credential.expiresAt!! < Date()) return null
            val payload = credential.payload
            val nonce = payload.getClaim("nonce").asLong()
            val userID = payload.getClaim("userID").asString()
            val session = fromNonce(nonce) ?: return null
            if (session.userID != userID) return null
            if (session.expirationTimestamp < UnixTimestamp.now()) return null
            if (session.createdTimestamp > UnixTimestamp.now()) return null
            val user = session.user ?: return null
            if(withAdmin && !user.admin && !user.superAdmin) return null
            var originalUserID: tUserID? = null
            val originalJWT = call.getDataFromAny("originalJWT")
            if(originalJWT != null){
                originalUserID = JWT.decode(originalJWT).getClaim("userID").asString()
            }
            return JWTSessionPrincipal(
                payload,
                session,
                userID,
                user,
                user.admin,
                user.superAdmin,
                originalUserID
            )
        }
    }


    override fun save(): Boolean {
        transaction {
            Session.upsert(Session.nonce) {
                it[userID] = this@Session.userID
                it[nonce] = this@Session.nonce
                it[userAgent] = this@Session.userAgent
                it[createdTimestamp] = this@Session.createdTimestamp.toDB()
                it[expirationTimestamp] = this@Session.expirationTimestamp.toDB()
                it[lastUsedTimestamp] = this@Session.lastUsedTimestamp.toDB()
            }
        }
        return true
    }

    override fun delete(): Boolean {
        transaction {
            Session.deleteWhere {
                (userID eq this@Session.userID) and
                        (nonce eq this@Session.nonce)
            }
        }
        return true
    }

    fun asSafeSession(currentSession: Session): SafeSession{
        return SafeSession(expirationTimestamp.toClient(), userAgent, nonce.toString(), currentSession.nonce == nonce)
    }
}