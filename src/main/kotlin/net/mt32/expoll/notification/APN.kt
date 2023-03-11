package net.mt32.expoll.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mt32.expoll.helper.minmax

enum class APNsPushType(val value: String) {
    /** Normal push notification */
    ALERT("alert"),

    /** Silent notification with no user interaction */
    BACKGROUND("background"),

    /** Silent notification with location update */
    LOCATION("location"),

    /** VoIP notification */
    VOIP("voip"),

    /** Notification for Apple Watch complication */
    COMPLICATION("complication"),

    /** Notification for File Provider */
    FILE_PROVIDER("fileprovider"),

    /** Notification for Mobile Device Management */
    MDM("mdm")
}

data class APNsPriority(var priority: Int) {
    init {
        this.priority = minmax(priority, 0, 10)
    }

    companion object {
        val low = APNsPriority(0)
        val medium = APNsPriority(5)
        val high = APNsPriority(10)
    }
}

@Serializable
data class APNsNotification(
    val title: String?,
    val subtitle: String?,
    val body: String?,
    @SerialName("launch-image") val launchImageName: String? = null,
    @SerialName("title-loc-key") val titleLocalisationKey: String? = null,
    @SerialName("title-loc-args") val titleLocalisationArgs: List<String>? = null,
    @SerialName("subtitle-loc-key") val subtitleLocalisationKey: String? = null,
    @SerialName("subtitle-loc-args") val subtitleLocalisationArgs: List<String>? = null,
    @SerialName("loc-key") val bodyLocalisationKey: String? = null,
    @SerialName("loc-args") val bodyLocalisationArgs: List<String>? = null
)

@Serializable
data class APS(
    val alert: APNsNotification,
    val badge: Int? = 0,
    val sound: String? = null
    // TODO add interruption level
)

@Serializable
data class APNsPayload(
    val aps: APS
)