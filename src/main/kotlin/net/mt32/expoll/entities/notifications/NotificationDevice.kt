package net.mt32.expoll.entities.notifications

import net.mt32.expoll.entities.Session
import net.mt32.expoll.commons.helper.UnixTimestamp
import net.mt32.expoll.notification.UniversalNotification

interface NotificationDevice {
    val sessionNonce: Long
    val session: Session?
    val creationTimestamp: UnixTimestamp

    val isValid: Boolean
        get() = session != null

    fun sendNotification(universalNotification: UniversalNotification)
}