package net.mt32.expoll.auth

import com.yubico.webauthn.CredentialRepository
import com.yubico.webauthn.RegisteredCredential
import com.yubico.webauthn.data.AuthenticatorTransport
import com.yubico.webauthn.data.ByteArray
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor
import net.mt32.expoll.entities.Authenticator
import net.mt32.expoll.entities.User
import java.util.*
import kotlin.jvm.optionals.getOrNull

object WebauthnRegistrationStorage : CredentialRepository {
    override fun getCredentialIdsForUsername(username: String?): MutableSet<PublicKeyCredentialDescriptor> {
        if (username == null) return mutableSetOf()
        val credentialIDs = Authenticator.fromUsername(username)
        return credentialIDs.map { auth ->
            PublicKeyCredentialDescriptor.builder()
                .id(ByteArray.fromBase64(auth.credentialID))
                /*.transports(auth.transports.map { transport ->
                    println(transport)
                    AuthenticatorTransport.values().find { it.id.equals(transport, ignoreCase = true) }
                }.toSet())*/
                .transports(AuthenticatorTransport.values().toSet())
                .build()
        }.toMutableSet()
    }

    override fun getUsernameForUserHandle(userHandle: ByteArray?): Optional<String> {
        if (userHandle == null) return Optional.empty()
        val user = User.fromUserHandle(userHandle) ?: return Optional.empty()
        return Optional.of(user.username)
    }

    override fun getUserHandleForUsername(username: String?): Optional<ByteArray> {
        if (username == null) return Optional.empty()
        val user = User.byUsername(username) ?: return Optional.empty()
        return Optional.of(user.userHandle)
    }

    override fun lookup(credentialId: ByteArray?, userHandle: ByteArray?): Optional<RegisteredCredential> {
        if (userHandle == null) return Optional.empty()
        val username = getUsernameForUserHandle(userHandle).getOrNull() ?: return Optional.empty()
        val user = User.byUsername(username)?: return Optional.empty()
        val auth = Authenticator.fromUser(user.id).find { it.credentialID == credentialId?.base64 }
            ?: return Optional.empty()
        return Optional.of(
            RegisteredCredential.builder()
                .credentialId(ByteArray.fromBase64(auth.credentialID))
                .userHandle(user.userHandle)
                .publicKeyCose(ByteArray.fromBase64(auth.credentialPublicKey))
                .signatureCount(auth.counter.toLong())
                .build()
        )
    }

    override fun lookupAll(credentialId: ByteArray?): Set<RegisteredCredential> {
        if (credentialId == null) return setOf()
        val base64 = credentialId.base64
        val auth = Authenticator.fromCredentialID(base64)
            ?: return setOf()
        return setOf(
            RegisteredCredential.builder()
                .credentialId(ByteArray.fromBase64(auth.credentialID))
                .userHandle(ByteArray.fromBase64(auth.userID))
                .publicKeyCose(ByteArray.fromBase64(auth.credentialPublicKey))
                .signatureCount(auth.counter.toLong())
                .build()
        )
    }
}