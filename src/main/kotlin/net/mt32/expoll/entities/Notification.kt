package net.mt32.expoll.entities

import kotlinx.serialization.Serializable


data class NotificationPreferences(
    val id: String,
    val user: User,
    var voteChange: Boolean = true,
    var userAdded: Boolean = true,
    var userRemoved: Boolean = true,
    var pollDeleted: Boolean = true,
    var polEdited: Boolean = true,
    var pollArchived: Boolean = true
)