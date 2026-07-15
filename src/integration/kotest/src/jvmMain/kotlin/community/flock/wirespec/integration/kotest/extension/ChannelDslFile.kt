package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.file

/** Builds the per-channel Kotest DSL file (`<Channel>Dsl.kt`) as an IR [File]. */
internal object ChannelDslFile {

    fun build(
        channel: Channel,
        packageName: PackageName,
        types: Map<String, Type> = emptyMap(),
        refined: Map<String, Refined> = emptyMap(),
    ): File {
        val shape = ChannelShape.from(channel, types, refined)
        val kotestPkg = "${packageName.value}.kotest"
        val channelPkg = "${packageName.value}.channel"
        val modelPkg = "${packageName.value}.model"
        val fileName = PackageName(kotestPkg).toDir() + "${shape.name}Dsl"

        return file(Name.of(fileName)) {
            `package`(kotestPkg)

            import("community.flock.wirespec.integration.kotest.dsl", "channelCall")
            import("community.flock.wirespec.integration.kotest.dsl", "WirespecScenarioDsl")
            // The payload builder pins fields as `Gen<…>`s; a field-less (primitive) payload
            // has no builder and no other `Gen` reference.
            if (shape.payloadFields.isNotEmpty()) {
                import("io.kotest.property", "Gen")
            }
            import("kotlin.time", "Duration")
            import(channelPkg, shape.name)
            shape.modelImports.forEach { import(modelPkg, it) }

            raw(renderGenerateExtension(shape))
            raw(renderMessageClass(shape))
            raw(renderCallClass(shape))
            if (shape.payloadFields.isNotEmpty()) {
                raw(renderPayloadBuilder(shape.payloadType, shape.payloadFields))
            }
        }
    }

    /**
     * The DSL entry point: a `generate` extension property on the generated channel object,
     * mirroring the endpoint DSL. The `<Channel>Generate` wrapper carries:
     * - `message` — materialises a random typed payload (per-field `Gen` overrides pin
     *   values, unset fields are generated) wrapped in a `<Channel>Message`; publishing
     *   happens exclusively by chaining the message's `send()`:
     *   `Queue.generate.message { … }.send()`;
     * - `call` — opens the `<Channel>Call` receiver for the receive direction, returning
     *   whatever the terminal (`expecting`/`collecting`/`returning`) yields.
     */
    private fun renderGenerateExtension(shape: ChannelShape): String = buildString {
        val payload = shape.payloadType
        appendLine("public class ${shape.name}Generate internal constructor() {")
        if (shape.payloadFields.isNotEmpty()) {
            appendLine("    public suspend fun message(block: ${payload}PayloadBuilder.() -> Unit = {}): ${shape.name}Message {")
            appendLine("        val builder = ${payload}PayloadBuilder().apply(block)")
            appendLine("        val payload = channelCall<$payload>(${shape.name}::class).buildMessageFields {")
            shape.payloadFields.forEach { f ->
                appendLine("            builder.${KotlinIdentifier.escape(f.name)}?.let { registerPath(\"${f.name}\") { it } }")
            }
            appendLine("        }")
            appendLine("        return ${shape.name}Message(payload)")
            appendLine("    }")
        } else {
            appendLine("    public suspend fun message(): ${shape.name}Message =")
            appendLine("        ${shape.name}Message(channelCall<$payload>(${shape.name}::class).buildMessage())")
        }
        appendLine("    public suspend fun <R> call(block: suspend ${shape.name}Call.() -> R): R =")
        appendLine("        ${shape.name}Call().block()")
        appendLine("}")
        appendLine("public val ${shape.name}.generate: ${shape.name}Generate")
        append("    get() = ${shape.name}Generate()")
    }

    /**
     * The materialised typed message returned by `generate.message { … }`. It carries the
     * built [payload]; `send()` publishes exactly that payload through the ambient channel
     * transport and returns it. `topic`/`key` pin the destination before sending.
     */
    private fun renderMessageClass(shape: ChannelShape): String = buildString {
        val payload = shape.payloadType
        val cls = "${shape.name}Message"
        appendLine("public class $cls internal constructor(public val payload: $payload) {")
        appendLine("    private val inner = channelCall<$payload>(${shape.name}::class)")
        appendLine("    public fun topic(value: String): $cls =")
        appendLine("        apply { inner.topic(value) }")
        appendLine("    public fun key(value: String): $cls =")
        appendLine("        apply { inner.key(value) }")
        appendLine("    public suspend fun send(): $payload =")
        appendLine("        inner.send(payload)")
        append("}")
    }

    /**
     * The receive-direction scope opened by `generate.call { … }`. Sending is not part of
     * this scope — it happens exclusively through `generate.message { … }.send()`.
     */
    private fun renderCallClass(shape: ChannelShape): String = buildString {
        val call = "${shape.name}Call"
        val payload = shape.payloadType
        appendLine("@WirespecScenarioDsl")
        appendLine("public class $call internal constructor() {")
        appendLine("    @PublishedApi internal val inner = channelCall<$payload>(${shape.name}::class)")
        appendLine("    public fun topic(value: String): $call =")
        appendLine("        apply { inner.topic(value) }")
        appendLine("    public fun key(value: String): $call =")
        appendLine("        apply { inner.key(value) }")
        appendLine("    public suspend fun expecting(): $payload =")
        appendLine("        inner.expecting()")
        appendLine("    public suspend fun expecting(block: ($payload) -> Unit): $payload =")
        appendLine("        inner.expecting(block)")
        appendLine("    public suspend fun collecting(count: Int, block: (List<$payload>) -> Unit): List<$payload> =")
        appendLine("        inner.collecting(count, block)")
        appendLine("    public suspend fun collecting(duration: Duration, block: (List<$payload>) -> Unit): List<$payload> =")
        appendLine("        inner.collecting(duration, block)")
        appendLine("    public suspend fun <T> returning(projection: ($payload) -> T): T =")
        append("        inner.returning(projection)\n}")
    }

    private fun renderPayloadBuilder(payloadType: String, fields: List<EndpointShape.NamedTypedField>): String = buildString {
        appendLine("@WirespecScenarioDsl")
        appendLine("public class ${payloadType}PayloadBuilder {")
        fields.forEach { f ->
            appendLine("    public var ${KotlinIdentifier.escape(f.name)}: Gen<${f.kotlinType}>? = null")
        }
        append("}")
    }
}
