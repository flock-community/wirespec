@file:OptIn(InternalSerializationApi::class)

package community.flock.wirespec.converter.asyncapi

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject

import kotlin.jvm.JvmInline

class AsyncApiModel {


    
    @JvmInline
    @Serializable
    value class Ref(val value: String)

    @Serializable
    data class Reference(
        @SerialName("\$ref")
        val ref: Ref
    ): SecurityOrReference

    @Serializable
    data class AsyncApi(
        val asyncapi: String,
        val info: Info,
        val servers: Map<String, Server>,
        val defaultContentType:String,
        val channels: Map<String, Channel>,
        val operations:  Map<String, Operation>,
        val components: Components,
    )

    @Serializable
    data class Info(
        val title: String,
        val version: String,
        val description: String,
        val termsOfService: String? = null,
        val contact: Contact? = null,
        val license: License,
        val tags: List<Tag> = emptyList(),
        val externalDocs: ExternalDocs? = null,
    ) {

        @Serializable
        data class Contact(
            val name: String,
            val email: String,
        )

        @Serializable
        data class License(
            val name: String,
            val url: String,
        )
    }

    @Serializable
    data class Server(
        val host: String,
        val pathname: String? = null,
        val protocol: String,
        val protocolVersion: String? = null,
        val description: String,
        val title: String? = null,
        val summary: String? = null,
        val security: List<SecurityOrReference>,
        val tags: List<Tag>,
        val binding: Map<String, Binding> = emptyMap(),
    )

    @Serializable
    data class Security(
        val type: String,
        val scheme: String,
    ):SecurityOrReference

    @Serializable
    data class Binding(
        val exchange: String,
        val queue: String,
    )

    @Serializable
    data class Channel(
        val address: String,
        val title: String? = null,
        val description: String? = null,
        val messages: Map<String, Reference>,
        val parameters: Map<String, Reference>,
        val servers: List<String> = emptyList(),
        val binding: Map<String, JsonObject> = emptyMap(),
        val tags: List<Tag> = emptyList(),
        val externalDocs: ExternalDocs? = null,
    )

    @Serializable
    data class Operation(
        val action: Action,
        val channel: Reference,
        val title: String? = null,
        val summary: String? = null,
        val description: String? = null,
        val security: List<JsonObject> = emptyList(),
        val tags: List<Tag> = emptyList(),
        val binding: Map<String, JsonObject> = emptyMap(),
        val traits: List<JsonObject>,
        val messages: List<JsonObject>,
        val reply: Reply? = null,
    ){
        @Serializable
        enum class Action {send, receive}

        @Serializable
        data class Reply(
            val message: Reference,
            val channel: Reference,
        )
    }

    @Serializable
    data class Components(
        val schemas: Map<String, JsonObject> = emptyMap(),
        val servers: Map<String, JsonObject> = emptyMap(),
        val channels: Map<String, Channel> = emptyMap(),
        val operations: Map<String, Operation> = emptyMap(),
        val messages: Map<String, JsonObject> = emptyMap(),
        val securitySchemes: Map<String, JsonObject> = emptyMap(),
        val serverVariables: Map<String, ServerVariable> = emptyMap(),
        val parameters: Map<String, Parameter> = emptyMap(),
        val correlationIds: Map<String, CorrelationId> = emptyMap(),
        val replies: Map<String, JsonObject> = emptyMap(),
        val replyAddresses: Map<String, JsonObject> = emptyMap(),
        val externalDocs: Map<String, JsonObject> = emptyMap(),
        val tags: Map<String, JsonObject> = emptyMap(),
        val operationTraits: Map<String, JsonObject> = emptyMap(),
        val messageTraits: Map<String, JsonObject> = emptyMap(),
        val serverBindings: Map<String, JsonObject> = emptyMap(),
        val channelBindings: Map<String, JsonObject> = emptyMap(),
        val operationBindings: Map<String, JsonObject> = emptyMap(),
        val messageBindings: Map<String, JsonObject> = emptyMap(),
    )

    @Serializable
    data class CorrelationId(
        val description: String? = null,
        val location: String,
    )

    @Serializable
    data class ServerVariable(
        val enum: List<String>,
        val default: String,
        val description: String,
        val examples: List<String>,
    )

    @Serializable
    data class Parameter(
        val enum: List<String> = emptyList(),
        val default: String? = null,
        val description: String,
        val examples: List<String> = emptyList(),
        val location: String? = null,
    )

    @Serializable
    data class Tag(
        val name: String,
        val description: String,
    )

    @Serializable
    data class ExternalDocs(
        val description: String,
        val url: String,
    )

    @Serializable(with = SecurityOrReferenceSerializer::class)
    sealed interface SecurityOrReference
    object SecurityOrReferenceSerializer : KSerializer<SecurityOrReference> {

        override val descriptor: SerialDescriptor =
            buildSerialDescriptor("SecurityOrReference", PolymorphicKind.SEALED)

        override fun serialize(encoder: Encoder, value: SecurityOrReference) {
            val serializer = when (value) {
                is Security -> Security.serializer()
                is Reference -> Reference.serializer()
            } as SerializationStrategy<SecurityOrReference>
            encoder.encodeSerializableValue(serializer, value)
        }

        override fun deserialize(decoder: Decoder): SecurityOrReference {
            val input = decoder as? JsonDecoder ?: throw SerializationException("This class can be loaded only by Json")
            val tree = input.decodeJsonElement().jsonObject
            return when {
                tree.containsKey("\$ref") -> input.json.decodeFromJsonElement(Reference.serializer(), tree)
                else -> input.json.decodeFromJsonElement(Security.serializer(), tree)
            }
        }
    }
}