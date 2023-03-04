package net.mt32.expoll

import kotlinx.serialization.Serializable

@Serializable
data class MailConfig(
    val mailServer: String,
    val mailPort: Int,
    val mailSecure: Boolean,
    val mailUser: String,
    val mailPassword: String
)

@Serializable
data class ConfigData(
    val mail: MailConfig
){

}
fun test(){

}