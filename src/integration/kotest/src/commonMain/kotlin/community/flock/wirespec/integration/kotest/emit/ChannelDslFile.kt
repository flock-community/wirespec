package community.flock.wirespec.integration.kotest.emit

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.ir.converter.convert
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Visibility
import community.flock.wirespec.ir.core.file
import community.flock.wirespec.ir.generator.KotlinGenerator
import community.flock.wirespec.ir.core.Type as IrType

/** Builds the per-channel Kotest DSL file (`<Channel>Dsl.kt`): the `<Channel>Generate` class, its `generate` extension property, and the `Gen<Payload>.send()` extension. */
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
            import("io.kotest.property", "Gen")
            import(channelPkg, shape.name)
            // pascalCase so underscore-bearing type names resolve to the emitted class.
            shape.modelImports.forEach { import(modelPkg, Name.of(it).pascalCase()) }

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
                getter = FunctionCall(name = Name.of("${shape.name}Generate")),
            )

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

internal data class ChannelShape(
    val name: String,
    val payloadType: String,
    /** Payload record fields (reused from the endpoint body machinery); empty for a primitive payload. */
    val payloadFieldShapes: List<EndpointShape.BodyFieldShape>,
    val modelImports: List<String>,
) {
    companion object {
        fun from(
            channel: Channel,
            types: Map<String, Type> = emptyMap(),
            refined: Map<String, Refined> = emptyMap(),
        ): ChannelShape {
            val payloadRef = channel.reference
            val payloadType = KotlinGenerator.generateType(payloadRef.convert())
            val payloadCustom = payloadRef as? Reference.Custom

            val payloadFieldShapes = payloadCustom
                ?.let { EndpointShape.extractBodyFields(it.value, types, refined, visited = emptySet()) }
                ?: emptyList()

            val directRefs = payloadCustom
                ?.let { types[it.value] }
                ?.shape?.value
                ?.map { it.reference }
                ?: emptyList()
            val modelImports = EndpointShape.modelImportsFor(listOf(payloadRef) + directRefs, payloadFieldShapes, types)

            return ChannelShape(
                name = channel.identifier.value,
                payloadType = payloadType,
                payloadFieldShapes = payloadFieldShapes,
                modelImports = modelImports,
            )
        }
    }
}
