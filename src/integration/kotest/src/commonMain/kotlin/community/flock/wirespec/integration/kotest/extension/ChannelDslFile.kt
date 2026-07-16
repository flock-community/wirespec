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
            // `message` returns `Gen<Payload>` and `send()` extends it.
            import("io.kotest.property", "Gen")
            import("kotlin.time", "Duration")
            import(channelPkg, shape.name)
            // pascalCase so underscore-bearing type names resolve to the emitted class.
            shape.modelImports.forEach { import(modelPkg, Name.of(it).pascalCase()) }

            // The payload field builder is the shared `<Type>Builder` (emitted by the payload's own
            // `<Type>Dsl.kt`); the message entry point references it by name.
            raw(renderGenerateExtension(shape))
            raw(renderSendExtension(shape))
            raw(renderCallClass(shape))
        }
    }

    /**
     * The DSL entry point: a `generate` extension property on the generated channel object,
     * mirroring the endpoint DSL. The `<Channel>Generate` wrapper carries:
     * - `message` — returns a `Gen<Payload>` (per-field `Gen` overrides pin values, unset fields
     *   are generated); publishing chains off the `Gen`'s `send()`: `Queue.generate.message { … }.send()`;
     * - `listen` — opens the `<Channel>Listen` receiver for the receive direction, returning
     *   whatever the terminal (`expecting`/`collecting`/`returning`) yields.
     */
    private fun renderGenerateExtension(shape: ChannelShape): String = buildString {
        val payload = shape.payloadType
        appendLine("public class ${shape.name}Generate internal constructor() {")
        if (shape.payloadFieldShapes.isNotEmpty()) {
            val builder = RecordBuilder.builderName(payload)
            appendLine("    public fun message(block: $builder.() -> Unit = {}): Gen<$payload> {")
            appendLine("        val builder = $builder().apply(block)")
            appendLine("        return channelCall<$payload>(${shape.name}::class).messageGen {")
            append(RecordBuilder.renderRegistration(shape.payloadFieldShapes, "builder", emptyList(), "            "))
            appendLine("        }")
            appendLine("    }")
        } else {
            appendLine("    public fun message(): Gen<$payload> =")
            appendLine("        channelCall<$payload>(${shape.name}::class).messageGen()")
        }
        appendLine("    public suspend fun <R> listen(block: suspend ${shape.name}Listen.() -> R): R =")
        appendLine("        ${shape.name}Listen().block()")
        appendLine("}")
        appendLine("public val ${shape.name}.generate: ${shape.name}Generate")
        append("    get() = ${shape.name}Generate()")
    }

    /**
     * `send()` extension on the payload `Gen`, so `generate.message { … }.send()` draws one payload
     * (seeded by the ambient `RandomSource`), publishes it through the ambient channel transport,
     * and returns it. `topic`/`key` optionally pin the destination.
     */
    private fun renderSendExtension(shape: ChannelShape): String = buildString {
        val payload = shape.payloadType
        appendLine("public suspend fun Gen<$payload>.send(topic: String? = null, key: String? = null): $payload {")
        appendLine("    val call = channelCall<$payload>(${shape.name}::class)")
        appendLine("    topic?.let { call.topic(it) }")
        appendLine("    key?.let { call.key(it) }")
        appendLine("    return call.send(this)")
        append("}")
    }

    /**
     * The receive-direction scope opened by `generate.listen { … }`. Sending is not part of
     * this scope — it happens exclusively through `generate.message { … }.send()`.
     */
    private fun renderCallClass(shape: ChannelShape): String = buildString {
        val listen = "${shape.name}Listen"
        val payload = shape.payloadType
        appendLine("@WirespecScenarioDsl")
        appendLine("public class $listen internal constructor() {")
        appendLine("    @PublishedApi internal val inner = channelCall<$payload>(${shape.name}::class)")
        appendLine("    public fun topic(value: String): $listen =")
        appendLine("        apply { inner.topic(value) }")
        appendLine("    public fun key(value: String): $listen =")
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
}
