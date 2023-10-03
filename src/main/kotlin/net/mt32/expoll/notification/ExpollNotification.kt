package net.mt32.expoll.notification

/*

interface IExpollNotificationType {
    val body: String
    val title: String
}

enum class ExpollNotificationType(override val body: String, override val title: String="\$poll") : IExpollNotificationType {
    EMPTY(""),
    STARTUP("notification.server.backend.update %@"),
    VoteChange("notification.vote.change %@ %@"),
    UserAdded("notification.user.added %@ %@"),
    UserRemoved("notification.user.removed %@ %@"),
    PollDeleted("notification.poll.delete %@"),
    PollEdited("notification.poll.edited %@"),
    PollArchived("notification.poll.archived %@");

    fun notificationArgs(poll: Poll, user: User?): List<String> {
        if(this == STARTUP) return listOf(config.serverVersion)
        val pollUpdate =
            this == PollArchived ||
                    this == PollDeleted ||
                    this == PollEdited ||
                    this == UserAdded ||
                    this == UserRemoved ||
                    this == VoteChange
        val userUpdate =
            this == UserAdded ||
                    this == UserRemoved ||
                    this == VoteChange
        val pollString = if (pollUpdate) poll.name else null
        val userString = if (userUpdate) (user?.firstName + " " + user?.lastName) else null
        return listOf(userString, pollString).filterNotNull()
    }
}

interface IExpollNotification {
    val type: ExpollNotificationType
}

data class ExpollNotification(
    override val type: ExpollNotificationType,
    val pollID: tPollID,
    val affectedUserID: tUserID?
) : IExpollNotification {
    override fun equals(other: Any?): Boolean {
        if (other !is ExpollNotification) return false
        return type == other.type && pollID == other.pollID && affectedUserID == other.affectedUserID
    }
}

fun userWantNotificationType(type: ExpollNotificationType, user: User): Boolean {
    val notificationPreferences = user.notificationPreferences
    return when (type) {
        ExpollNotificationType.EMPTY -> false
        ExpollNotificationType.STARTUP -> user.admin
        ExpollNotificationType.VoteChange -> notificationPreferences.voteChange
        ExpollNotificationType.UserAdded -> notificationPreferences.userAdded
        ExpollNotificationType.UserRemoved -> notificationPreferences.userRemoved
        ExpollNotificationType.PollDeleted -> notificationPreferences.pollDeleted
        ExpollNotificationType.PollEdited -> notificationPreferences.pollEdited
        ExpollNotificationType.PollArchived -> notificationPreferences.pollArchived
    }
}

@Serializable
@SerialName("expollPayload")
class ExpollAPNsPayload(
    override val aps: APS,
    val pollID: tPollID? = null
) : IAPNsPayload

var lastNotification: ExpollNotification = ExpollNotification(ExpollNotificationType.EMPTY, "", null)
var lastNotificationTime: UnixTimestamp = UnixTimestamp.zero()

fun sendNotificationAllowed(notification: ExpollNotification): Boolean {
    if (config.developmentMode) return false
    if (lastNotification == notification && lastNotificationTime.addMinutes(1) > UnixTimestamp.now()) {
        return false
    }
    lastNotification = notification
    lastNotificationTime = UnixTimestamp.now()
    return true
}

@OptIn(DelicateCoroutinesApi::class)
@Deprecated("use ExpollNotificationHandler.sendNotification instead")
fun sendNotification(notification: ExpollNotification) {
    //if (!sendNotificationAllowed(notification)) return
    //AnalyticsStorage.notificationCount[notification.type] =
    //    (AnalyticsStorage.notificationCount[notification.type] ?: 0) + 1
    GlobalScope.launch {
        val poll = Poll.fromID(notification.pollID)
        val affectedUser = notification.affectedUserID?.let { User.loadFromID(it) }
        if (poll == null) return@launch
        val apnsNotification = APNsNotification(
            notification.type.title.replace("\$poll", "Poll ${poll.name} was updated"),
            null,
            null,
            bodyLocalisationKey = notification.type.body,
            bodyLocalisationArgs = notification.type.notificationArgs(poll, affectedUser)
        )
        val payload = ExpollAPNsPayload(APS(apnsNotification), poll.id)
        val expiration = UnixTimestamp.now().addDays(5)
        poll.users.forEach { user ->
            if (!userWantNotificationType(notification.type, user)) return@forEach

            sendNotification(payload, user, expiration, APNsPriority.medium)
        }
    }
}

@Deprecated("use ExpollNotificationHandler.sendNotification instead")
fun sendNotification(payload: IAPNsPayload, user: User, expiration: UnixTimestamp, priority: APNsPriority) {
    sendNotification(payload, user.apnDevices, expiration, priority)
}

@Deprecated("use ExpollNotificationHandler.sendNotification instead")
fun sendNotification(
    payload: IAPNsPayload,
    devices: List<APNDevice>,
    expiration: UnixTimestamp,
    priority: APNsPriority
) {
    devices.forEach { device ->
        sendNotification(payload, device, expiration, priority)
    }
}

@Deprecated("use ExpollNotificationHandler.sendNotification instead")
fun sendNotification(payload: IAPNsPayload, device: APNDevice, expiration: UnixTimestamp, priority: APNsPriority) {
    if (device.session == null)
        device.delete()
    else
        runBlocking {
            APNsNotificationHandler.sendAPN(device.deviceID, expiration, payload, priority)
        }
}*/

/*

fun generateAESKey(): SecretKey? {
    val aesKeyInst = KeyGenerator.getInstance("AES")
    aesKeyInst.init(128)
    return aesKeyInst.generateKey()
}

fun generateAESKey(sharedSecret: ByteArray, authSecret: ByteArray): SecretKey {
    // Combine the shared secret and auth secret to generate the AES key
    val combinedSecret = sharedSecret + authSecret
    return SecretKeySpec(combinedSecret, "AES")
}

fun encryptAESGCM(
    data: ByteArray,
    userPublicKey: ByteArray,
    privateKey: ByteArray,
    salt: ByteArray,
    authSecret: ByteArray
): ByteArray {
    // Derive a shared secret from userPublicKey and privateKey
    val sharedSecret = deriveSharedSecret(userPublicKey, privateKey, salt)

    // Generate an AES key from the shared secret and authSecret
    val aesKey = generateAESKey(sharedSecret, authSecret)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val iv = ByteArray(12) // IV (Initialization Vector) should be 12 bytes for AES-GCM
    SecureRandom().nextBytes(iv)

    val gcmParameterSpec = GCMParameterSpec(128, iv)
    cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmParameterSpec)

    val encryptedData = cipher.doFinal(data)
    return iv + encryptedData // Combine IV and encrypted data
}

fun deriveSharedSecret(userPublicKey: ByteArray, privateKey: ByteArray, salt: ByteArray): ByteArray {
    // Convert userPublicKey to a PublicKey object
    val publicKeySpec = X509EncodedKeySpec(userPublicKey)
    val publicKeyFactory = KeyFactory.getInstance("EC")
    val publicKey = publicKeyFactory.generatePublic(publicKeySpec)

    // Convert privateKey to a PrivateKey object
    val privateKeySpec = PKCS8EncodedKeySpec(privateKey)
    val privateKeyFactory = KeyFactory.getInstance("EC")
    val privateKeyObj = privateKeyFactory.generatePrivate(privateKeySpec)

    val localKeysCurve = KeyPairGenerator.getInstance("EC").generateKeyPair()


    val sharedSecret = performECDHKeyAgreement(publicKey, localKeysCurve.private)

    return sharedSecret // Replace with the actual derived sharedSecret
}

fun performECDHKeyAgreement(publicKey: PublicKey, privateKey: PrivateKey): ByteArray {
    val sharedSecret: ByteArray

    // Perform ECDH Key Agreement
    val keyAgreement = KeyAgreement.getInstance("ECDH")
    keyAgreement.init(privateKey)
    keyAgreement.doPhase(publicKey, true)

    // Generate the shared secret
    sharedSecret = keyAgreement.generateSecret()

    return sharedSecret
}

suspend fun sendWebNotification(){
    val notificationDevice: WebNotificationDevice = WebNotificationDevice("test", "test", "test", "test", UnixTimestamp.now(), 0)
    val privateApplicationServerKey = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(config.notifications.privateApplicationServerKey.toByteArray()))
    val publicApplicationServerKey = config.notifications.publicApplicationServerKey
    val subject = "mailto:programming@universegame.de"
    val jwt = JWT.create()
        .withAudience(notificationDevice.endpoint.getHostPartFromURL())
        .withIssuer("Expoll")
        .withSubject(subject)
        .withExpiresAt(UnixTimestamp.now().addDays(5).toDate())
        .sign(Algorithm.ECDSA256(privateApplicationServerKey.toECPrivateKey()))

    val message = "test"
    val salt = Random.nextBytes(16).toBase64()
    // create local prime256v1 keypair
    val keyPairGen = KeyPairGenerator.getInstance("EC")
    val spec = ECGenParameterSpec("prime256v1")
    keyPairGen.initialize(spec)
    val keyPair = keyPairGen.generateKeyPair()
    val publicKey = keyPair.public
    val privateKey = keyPair.private
    // encrypt message  AES_128_GCM
    val encryptedMessage = encryptAESGCM(message.toByteArray(), notificationDevice.p256dh.toByteArray(), privateKey.encoded, salt.toByteArray(), notificationDevice.auth.toByteArray())
    val client = HttpClient {  }
    client.request(URL(notificationDevice.endpoint)){
        method = HttpMethod.Post
        headers{
            append("Authorization", "WebPush ${jwt}")
            append("Crypto-Key", "p256ecdsa=${publicApplicationServerKey};dh=${publicKey.encoded.toBase64()}")
            append("Content-Encoding", "aesgcm")
            append("Encryption", "keyid=p256ecdsa;salt=${salt}")
            append("TTL", "60")
            append("Content-Type", "application/octet-stream")
        }
        setBody(encryptedMessage)
    }
}*/

