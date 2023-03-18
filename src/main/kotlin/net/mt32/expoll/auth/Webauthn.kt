package net.mt32.expoll.auth

import com.yubico.webauthn.CredentialRepository
import com.yubico.webauthn.RegisteredCredential
import com.yubico.webauthn.data.AuthenticatorTransport
import com.yubico.webauthn.data.ByteArray
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor
import net.mt32.expoll.entities.Authenticator
import net.mt32.expoll.entities.User
import net.mt32.expoll.helper.base64ToByteArray
import net.mt32.expoll.helper.toBase64
import java.util.*

object WebauthnRegistrationStorage : CredentialRepository {
    override fun getCredentialIdsForUsername(username: String?): MutableSet<PublicKeyCredentialDescriptor> {
        if (username == null) return mutableSetOf()
        val user = User.byUsername(username) ?: return mutableSetOf()
        return Authenticator.fromUser(user.id).map { auth ->
            PublicKeyCredentialDescriptor.builder()
                .id(ByteArray(auth.credentialID.toByteArray()))
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
        val id = userHandle.bytes.toBase64()
        val user = User.loadFromID(id) ?: return Optional.empty()
        return Optional.of(user.username)
    }

    override fun getUserHandleForUsername(username: String?): Optional<ByteArray> {
        if (username == null) return Optional.empty()
        val user = User.byUsername(username) ?: return Optional.empty()
        return Optional.of(ByteArray(user.id.toByteArray()))
    }

    override fun lookup(credentialId: ByteArray?, userHandle: ByteArray?): Optional<RegisteredCredential> {
        if (userHandle == null) return Optional.empty()
        val id = userHandle.bytes.toBase64()
        val user = User.loadFromID(id) ?: return Optional.empty()
        val auth = Authenticator.fromUser(user.id).find { it.credentialID == credentialId?.bytes?.toBase64() }
            ?: return Optional.empty()
        return Optional.of(
            RegisteredCredential.builder()
                .credentialId(ByteArray(auth.credentialID.base64ToByteArray()))
                .userHandle(ByteArray(user.id.toByteArray()))
                .publicKeyCose(ByteArray(auth.credentialPublicKey.base64ToByteArray()))
                .signatureCount(auth.counter.toLong())
                .build()
        )
    }

    override fun lookupAll(credentialId: ByteArray?): Set<RegisteredCredential> {
        if (credentialId == null) return setOf()
        val auth = credentialId.bytes?.toBase64()?.let { Authenticator.fromCredentialID(it) }
            ?: return setOf()
        return setOf(
            RegisteredCredential.builder()
                .credentialId(ByteArray(auth.credentialID.base64ToByteArray()))
                .userHandle(ByteArray(auth.userID.toByteArray()))
                .publicKeyCose(ByteArray(auth.credentialPublicKey.base64ToByteArray()))
                .signatureCount(auth.counter.toLong())
                .build()
        )
    }

}