package community.flock.wirespec.converter.asyncapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

import kotlin.jvm.JvmInline

class AsyncApiModel {

    @JvmInline
    @Serializable
    value class Ref(val value: String)

    @Serializable
    data class ReferenceObject(
        @SerialName("\$ref")
        val ref: Ref
    )

    @Serializable
    data class AsyncApi(
        val info: Info,
        val servers: Map<String, Server>,
        val channels: Channels,
        val operations: Operations,
        val components: Components,
    )

    @Serializable
    data class Info(
        val title: String,
        val version: String,
        val description: String,
        val termsOfService: String,
        val contact: Contact,
        val license: License,
        val tags: List<Tag>,
        val externalDocs: ExternalDocs,
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
        val pathname: String,
        val protocol: String,
        val protocolVersion: String,
        val description: String,
        val title: String,
        val summary: String,
        val security: List<Security>,
        val tags: List<Tag>,
        val binding: Map<String, Binding>,
    ) {
        @Serializable
        data class Security(
            val type: String,
            val scheme: String,
        )

        @Serializable
        data class Binding(
            val exchange: String,
            val queue: String,
        )

    }

    @Serializable
    data class Channels(
        val address: String,
        val title: String,
        val description: String,
        val messages: Map<String, ReferenceObject>,
        val parameters: Map<String, ReferenceObject>,
        val servers: List<String>,
        val binding: Map<String, JsonObject>,
        val tags: List<Tag>,
        val externalDocs: ExternalDocs,
    )

    @Serializable
    data class Operations(
        val action: Action,
        val channel: ReferenceObject,
        val security: List<Json>,
        val tags: List<Tag>,
        val binding: Map<String, JsonObject>,
        val traits: List<JsonObject>,
        val messages: List<JsonObject>,
        val reply: Reply,
    ){
        enum class Action {send, receive}
        data class Reply(
            val message: ReferenceObject,
            val channel: ReferenceObject,
        )
    }

    data class Components(

    )

    data class Tag(
        val name: String,
        val description: String,
    )

    data class ExternalDocs(
        val description: String,
        val url: String,
    )
}