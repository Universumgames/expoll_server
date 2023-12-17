package net.mt32.expoll.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import net.mt32.expoll.auth.JWTSessionPrincipal
import net.mt32.expoll.entities.NotificationPreferences
import net.mt32.expoll.entities.NotificationPreferencesSerial
import net.mt32.expoll.entities.notifications.APNDevice
import net.mt32.expoll.entities.notifications.WebNotificationDevice
import net.mt32.expoll.helper.*

fun Route.notificationRoutes() {
    route("notifications") {
        route("/preferences") {
            get {
                getNotifications(call)
            }
            post {
                setNotification(call)
            }
        }
        route("/apple"){
            post{
                registerAppleDevice(call)
            }
            delete {

            }
        }
        route("web"){
            post {
                registerWebDevice(call)
            }
            delete {

            }
        }
    }
}

private suspend fun registerAppleDevice(call:ApplicationCall){
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }

    val deviceID = call.getDataFromAny("deviceID")
    if(deviceID == null){
        call.respond(ReturnCode.MISSING_PARAMS)
        return
    }
    val existingDevice = APNDevice.fromDeviceID(deviceID)
    if(existingDevice != null){
        existingDevice.userID = principal.userID
        existingDevice.sessionNonce = principal.session.nonce
        existingDevice.save()
        call.respond(ReturnCode.OK)
        return
    }
    val newDevice = APNDevice(deviceID, principal.userID, UnixTimestamp.now(), principal.session.nonce)
    newDevice.save()

    call.respond(ReturnCode.OK)
}

private suspend fun unregisterAppleDevice(call: ApplicationCall){

}

private suspend fun setNotification(call: ApplicationCall){
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }
    val sentPreferences: NotificationPreferencesSerial = defaultJSON.decodeFromString(call.receiveText())
    val originalPreferences = NotificationPreferences.fromUser(principal.userID)

    originalPreferences.pollArchived = sentPreferences.pollArchived == true
    originalPreferences.pollDeleted = sentPreferences.pollDeleted == true
    originalPreferences.pollEdited = sentPreferences.pollEdited == true
    originalPreferences.userAdded = sentPreferences.userAdded == true
    originalPreferences.userRemoved = sentPreferences.userRemoved == true
    originalPreferences.voteChange = sentPreferences.voteChange == true
    originalPreferences.voteChangeDetailed = sentPreferences.voteChangeDetailed == true
    originalPreferences.save()

    call.respond(ReturnCode.OK)
}

private suspend fun getNotifications(call: ApplicationCall) {
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }
    call.respond(NotificationPreferences.fromUser(principal.userID).toSerializable())
}

@Serializable
data class WebRegistrationData(
    val endpoint: String,
    val expirationTime: Long? = null,
    val keys: WebRegistrationKeys
)

@Serializable
data class WebRegistrationKeys(
    val p256dh: String,
    val auth: String
)

private suspend fun registerWebDevice(call: ApplicationCall){
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }

    val registrationData: WebRegistrationData = call.receive()
    val existingDevice = WebNotificationDevice.fromEndpoint(registrationData.endpoint)
    if(existingDevice != null){
        existingDevice.p256dh = registrationData.keys.p256dh
        existingDevice.auth = registrationData.keys.auth
        existingDevice.save()
        call.respond(ReturnCode.OK)
        return
    }

    val newDevice = WebNotificationDevice(
        registrationData.endpoint,
        registrationData.keys.auth,
        registrationData.keys.p256dh,
        principal.userID,
        registrationData.expirationTime?.toUnixTimestampFromClient(),
        UnixTimestamp.now(),
        principal.session.nonce
    )
    newDevice.save()

    call.respond(ReturnCode.OK)
}