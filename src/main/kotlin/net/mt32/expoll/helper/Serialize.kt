package net.mt32.expoll.helper

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.text.SimpleDateFormat
import java.util.*

@Serializer(forClass = Date::class)
object DateSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    override fun deserialize(decoder: Decoder): Date {
        return formatter.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Date) {
        return encoder.encodeString(formatter.format(value))
    }

}

