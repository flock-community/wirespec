package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Visibility
import community.flock.wirespec.ir.core.file
import community.flock.wirespec.ir.core.Type as IrType

/**
 * Builds the per-channel Kotest DSL file (`<Channel>Dsl.kt`) as an IR [File] using the IR
 * DSL builders: the `<Channel>Generate` class, its `generate` extension property, and the
 * `Gen<Payload>.send()` extension. Only the imperative method/getter bodies are raw code.
 */
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

        val payload = shape.payloadType
        val genPayload = IrType.Custom("Gen", listOf(IrType.Custom(payload)))

        return file(Name.of(fileName)) {
            `package`(kotestPkg)

            import("community.flock.wirespec.integration.kotest.dsl", "channelCall")
            // `message` returns `Gen<Payload>` and `send()` extends it.
            import("io.kotest.property", "Gen")
            import(channelPkg, shape.name)
            // pascalCase so underscore-bearing type names resolve to the emitted class.
            shape.modelImports.forEach { import(modelPkg, Name.of(it).pascalCase()) }

            // The DSL entry point: a `generate` extension property on the generated channel object,
            // mirroring the endpoint DSL. The `<Channel>Generate` wrapper carries `message` — it
            // returns a `Gen<Payload>` (per-field `Gen` overrides pin values, unset fields are
            // generated). The payload field builder is the shared `<Type>Builder` (emitted by the
            // payload's own `<Type>Dsl.kt`); the message entry point references it by name.
            struct("${shape.name}Generate") {
                plainClass()
                visibility(Visibility.PUBLIC)
                constructorVisibility(Visibility.INTERNAL)
                if (shape.payloadFieldShapes.isNotEmpty()) {
                    val builder = RecordBuilder.builderName(payload)
                    function("message") {
                        visibility(Visibility.PUBLIC)
                        arg("block", functionType(returnType = IrType.Unit, receiver = IrType.Custom(builder)), rawExpr("{}"))
                        returnType(genPayload)
                        raw("val builder = $builder().apply(block)")
                        raw("return channelCall<$payload>(${shape.name}::class).messageGen {")
                        raw(RecordBuilder.renderRegistration(shape.payloadFieldShapes, "builder", emptyList(), "    ").trimEnd())
                        raw("}")
                    }
                } else {
                    function("message") {
                        visibility(Visibility.PUBLIC)
                        returnType(genPayload)
                        returns(rawExpr("channelCall<$payload>(${shape.name}::class).messageGen()"))
                    }
                }
            }

            property(
                name = "generate",
                type = IrType.Custom("${shape.name}Generate"),
                visibility = Visibility.PUBLIC,
                receiver = IrType.Custom(shape.name),
                getter = rawExpr("${shape.name}Generate()"),
            )

            // `send()` extension on the payload `Gen`, so `generate.message { … }.send()` draws one
            // payload (seeded by the ambient `RandomSource`), publishes it through the ambient channel
            // transport, and returns it. `topic`/`key` optionally pin the destination.
            asyncFunction("send") {
                visibility(Visibility.PUBLIC)
                receiver(genPayload)
                arg("topic", IrType.Nullable(IrType.String), rawExpr("null"))
                arg("key", IrType.Nullable(IrType.String), rawExpr("null"))
                returnType(IrType.Custom(payload))
                raw("val call = channelCall<$payload>(${shape.name}::class)")
                raw("topic?.let { call.topic(it) }")
                raw("key?.let { call.key(it) }")
                returns(rawExpr("call.send(this)"))
            }
        }
    }
}
