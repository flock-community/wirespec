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
            import("io.kotest.property", "Gen")
            import("kotlin.time", "Duration")
            import(channelPkg, shape.name)
            shape.modelImports.forEach { import(modelPkg, it) }

            raw(renderGenerateExtension(shape))
            raw(renderCallClass(shape))
            if (shape.payloadFields.isNotEmpty()) {
                raw(renderPayloadBuilder(shape.payloadType, shape.payloadFields))
            }
        }
    }

    /**
     * The DSL entry point: a `generate` extension property on the generated channel object
     * (e.g. `Queue.generate.call { … }`), mirroring the endpoint DSL. Its `call` member opens
     * the `<Channel>Call` receiver and runs the block, returning whatever the terminal
     * (`send`/`expecting`/`collecting`/`returning`) yields.
     */
    private fun renderGenerateExtension(shape: ChannelShape): String = buildString {
        appendLine("public class ${shape.name}Generate internal constructor() {")
        appendLine("    public suspend fun <R> call(block: suspend ${shape.name}Call.() -> R): R =")
        appendLine("        ${shape.name}Call().block()")
        appendLine("}")
        appendLine("public val ${shape.name}.generate: ${shape.name}Generate")
        append("    get() = ${shape.name}Generate()")
    }

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
        appendLine("    public suspend fun send(): $payload =")
        appendLine("        inner.send()")
        appendLine("    public suspend fun send(gen: Gen<$payload>): $payload =")
        appendLine("        inner.send(gen)")
        if (shape.payloadFields.isNotEmpty()) {
            appendLine("    public suspend fun send(block: ${payload}PayloadBuilder.() -> Unit): $payload {")
            appendLine("        val builder = ${payload}PayloadBuilder().apply(block)")
            appendLine("        return inner.sendFields {")
            shape.payloadFields.forEach { f ->
                appendLine("            builder.${KotlinIdentifier.escape(f.name)}?.let { registerPath(\"${f.name}\") { it } }")
            }
            appendLine("        }")
            appendLine("    }")
        }
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
