package net.mt32.expoll.plugins

import io.ktor.serialization.kotlinx.cbor.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.serialization.kotlinx.protobuf.*
import io.ktor.serialization.kotlinx.xml.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.protobuf.ProtoBuf
import net.mt32.expoll.helper.defaultJSON

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(json = defaultJSON)
        xml()
        cbor(Cbor {
            ignoreUnknownKeys = true
        })
        protobuf(ProtoBuf {
            encodeDefaults = true
        })
    }
}
