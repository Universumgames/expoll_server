package net.mt32.expoll.database.transform

import net.mt32.expoll.config
import net.mt32.expoll.database.Transformer
import net.mt32.expoll.entities.User

fun Transformer.renameOIDCUsernames() {
    val users = User.all()
    for (user in users) {
        val username = user.username
        val unnamedOIDCUser =
            config.oidc.idps.keys.any { oidcProvider ->
                if (username.startsWith(oidcProvider)) {
                    return@any true
                }
                false
            }
        if (unnamedOIDCUser) {
            val newUsername = User.getUniqueUsername(null)
            user.username = newUsername
            user.save()
        }
    }
}