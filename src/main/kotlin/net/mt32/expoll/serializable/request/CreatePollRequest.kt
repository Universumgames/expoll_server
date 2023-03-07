package net.mt32.expoll.serializable.request

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import net.mt32.expoll.serializable.responses.PollOptions

@Serializable
data class CreatePollRequest(
    val name: String,
    val maxPerUserVoteCount: Int,
    val description: String,
    val type: Int,
    val options: JsonElement,
    val allowsMaybe: Boolean,
    val allowsEditing: Boolean,
)