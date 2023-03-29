package net.mt32.expoll.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.decodeFromString
import net.mt32.expoll.auth.JWTSessionPrincipal
import net.mt32.expoll.entities.APNDevice
import net.mt32.expoll.entities.NotificationPreferences
import net.mt32.expoll.helper.ReturnCode
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.helper.defaultJSON
import net.mt32.expoll.helper.getDataFromAny

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
    val sentPreferences: NotificationPreferences = defaultJSON.decodeFromString(call.receiveText())
    val originalPreferences = NotificationPreferences.fromUser(principal.userID)

    originalPreferences.pollArchived = sentPreferences.pollArchived
    originalPreferences.pollDeleted = sentPreferences.pollDeleted
    originalPreferences.pollEdited = sentPreferences.pollEdited
    originalPreferences.userAdded = sentPreferences.userAdded
    originalPreferences.userRemoved = sentPreferences.userRemoved
    originalPreferences.voteChange = sentPreferences.voteChange
    originalPreferences.save()

    call.respond(ReturnCode.OK)
}

private suspend fun getNotifications(call: ApplicationCall) {
    val principal = call.principal<JWTSessionPrincipal>()
    if (principal == null) {
        call.respond(ReturnCode.UNAUTHORIZED)
        return
    }
    call.respond(NotificationPreferences.fromUser(principal.userID))
}