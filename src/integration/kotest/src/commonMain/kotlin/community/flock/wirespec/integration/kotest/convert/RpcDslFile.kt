package community.flock.wirespec.integration.kotest.convert

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.ConstructorStatement
import community.flock.wirespec.compiler.core.ir.Expression
import community.flock.wirespec.compiler.core.ir.File
import community.flock.wirespec.compiler.core.ir.FileBuilder
import community.flock.wirespec.compiler.core.ir.FunctionCall
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.ReturnStatement
import community.flock.wirespec.compiler.core.ir.StructBuilder
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.Visibility
import community.flock.wirespec.compiler.core.ir.converter.convert
import community.flock.wirespec.compiler.core.ir.converter.convertToGenerator
import community.flock.wirespec.compiler.core.ir.file
import community.flock.wirespec.compiler.core.ir.findAll
import community.flock.wirespec.compiler.core.ir.generator.KotlinGenerator
import community.flock.wirespec.compiler.core.ir.generator.escapeKotlinIdentifier
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Shape
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.fields
import community.flock.wirespec.compiler.core.ir.Function as IrFunction
import community.flock.wirespec.compiler.core.ir.Type as IrType

internal object RpcDslFile {

    fun build(
        rpc: Rpc,
        packageName: PackageName,
        module: Module? = null,
        types: Map<String, Type> = emptyMap(),
        refined: Map<String, Refined> = emptyMap(),
    ): File {
        val shape = RpcShape.from(rpc, types, refined)
        val kotestPkg = "${packageName.value}.kotest"
        val fileName = PackageName(kotestPkg).toDir() + "${shape.name}Dsl"

        return file(Name.of(fileName)) {
            `package`(kotestPkg)

            if (shape.generatedValues.isNotEmpty()) {
                import("community.flock.wirespec.integration.kotest.dsl", "generatedGen")
                import("community.flock.wirespec.kotlin", "Wirespec")
            }
            import("community.flock.wirespec.integration.kotest.dsl", "rpcCall")
            if (shape.requestFields.isNotEmpty()) {
                import("community.flock.wirespec.integration.kotest.dsl", "WirespecScenarioDsl")
                import("community.flock.wirespec.integration.kotest.dsl", "draw")
                import("io.kotest.property", "RandomSource")
            }
            import("io.kotest.property.arbitrary", "constant")
            if (shape.generatorImports.isNotEmpty()) {
                import("kotlin.reflect", "typeOf")
            }
            import("io.kotest.property", "Arb")
            import("io.kotest.property", "Gen")
            import("${packageName.value}.rpc", shape.name)
            shape.modelImports.forEach { import("${packageName.value}.model", Name.of(it).pascalCase()) }
            shape.generatorImports.forEach { import("${packageName.value}.generator", "${Name.of(it).pascalCase()}Generator") }

            buildRequestClass(shape)
            buildGenerateWrapper(shape)
            property(
                name = "generate",
                type = IrType.Custom("${shape.name}Generate"),
                visibility = Visibility.PUBLIC,
                receiver = IrType.Custom(shape.name),
                getter = FunctionCall(name = Name.of("${shape.name}Generate")),
            )
            buildCall(shape)
            if (shape.requestFields.isNotEmpty()) {
                elements.add(RecordBuilder.buildBuilderClass(shape.requestName, shape.requestFields))
            }
            shape.generatedValues.forEach { buildValueGenerator(it, module) }
        }
    }

    private fun FileBuilder.buildRequestClass(shape: RpcShape) {
        struct(shape.requestName) {
            visibility(Visibility.PUBLIC)
            shape.parameters.forEach { field(Name(listOf(it.identifier.value.escapeKotlinIdentifier())), it.reference.convert()) }
            if (shape.parameters.isEmpty()) constructo {}
        }
    }

    private fun FileBuilder.buildGenerateWrapper(shape: RpcShape) {
        struct("${shape.name}Generate") {
            plainClass()
            visibility(Visibility.PUBLIC)
            constructorVisibility(Visibility.INTERNAL)
            val requestArb = IrType.Custom("Arb", listOf(IrType.Custom(shape.requestName)))
            if (shape.requestFields.isNotEmpty()) {
                val builder = RecordBuilder.builderName(shape.requestName)
                function("request") {
                    visibility(Visibility.PUBLIC)
                    arg("block", functionType(returnType = IrType.Unit, receiver = IrType.Custom(builder)), rawExpr("{}"))
                    returnType(requestArb)
                    raw("val builder = $builder().apply(block)")
                    raw("return generatedGen { generator, rs -> builder.applyTo(${RpcShape.GeneratedValue.call(shape.requestName)}, rs) }")
                }
            } else {
                function("request") {
                    visibility(Visibility.PUBLIC)
                    returnType(requestArb)
                    returns(rawExpr("Arb.constant(${shape.requestName})"))
                }
            }
            outcomeFunction("result", shape.result)
            shape.error?.let { outcomeFunction("error", it) }
        }
    }

    private fun StructBuilder.outcomeFunction(name: String, outcome: RpcShape.Outcome) {
        val value = outcome.generated?.let { RpcShape.GeneratedValue.call(it.name) }
        val wrap = { drawn: String -> outcome.wrapper?.let { "$it($drawn)" } ?: drawn }
        val record = outcome.record
        function(name) {
            visibility(Visibility.PUBLIC)
            returnType(IrType.Custom("Arb", listOf(IrType.Custom(outcome.type))))
            when {
                value == null -> returns(rawExpr("Arb.constant(${wrap("Unit")})"))
                record == null -> returns(rawExpr("generatedGen { generator, _ -> ${wrap(value)} }"))
                else -> {
                    val builder = RecordBuilder.builderName(record.typeName)
                    arg("block", functionType(returnType = IrType.Unit, receiver = IrType.Custom(builder)), rawExpr("{}"))
                    raw("val builder = $builder().apply(block)")
                    raw("return generatedGen { generator, rs -> ${wrap(record.applyBuilder(value, "builder"))} }")
                }
            }
        }
    }

    private fun FileBuilder.buildCall(shape: RpcShape) {
        val args = shape.parameters.joinToString(", ") { "request.${it.identifier.value.escapeKotlinIdentifier()}" }
        val request = if (shape.parameters.isEmpty()) "_" else "request"
        asyncFunction("call") {
            visibility(Visibility.PUBLIC)
            receiver(IrType.Custom("Gen", listOf(IrType.Custom(shape.requestName))))
            returnType(IrType.Custom(shape.callReturnType))
            returns(rawExpr("rpcCall(${shape.name}.Service::class, this) { service, $request -> service.${shape.functionName}($args) }"))
        }
    }

    private fun FileBuilder.buildValueGenerator(value: RpcShape.GeneratedValue, module: Module?) {
        function(RpcShape.GeneratedValue.functionName(value.name)) {
            visibility(Visibility.PRIVATE)
            arg("generator", IrType.Custom("Wirespec.Generator"))
            arg("path", IrType.Array(IrType.String))
            returnType(IrType.Custom(value.type))
            returns(value.expression(module))
        }
    }
}

internal data class RpcShape(
    val name: String,
    val functionName: String,
    val requestName: String,
    val parameters: List<Field>,
    val requestFields: List<EndpointShape.BodyFieldShape>,
    val result: Outcome,
    val error: Outcome?,
    val callReturnType: String,
    val modelImports: List<String>,
    val generatorImports: List<String>,
) {
    /** Drawn through the IR-emitted model generators, so it follows the same rules as a model field. */
    sealed interface GeneratedValue {
        val name: String
        val type: String
        fun expression(module: Module?): Expression

        data class Request(override val name: String, val parameters: List<Field>) : GeneratedValue {
            override val type: String = name

            override fun expression(module: Module?): Expression = generatorConstructor(name, parameters, module).let { constructed ->
                constructed.copy(namedArguments = constructed.namedArguments.mapKeys { (key, _) -> Name(listOf(key.value().escapeKotlinIdentifier())) })
            }
        }

        // Generated as the single field of a synthesized record, then lifted to the root path, so the fields
        // of a record result sit at `name` (as with `<Type>.generate`), not at `value/name`.
        data class Value(override val name: String, override val type: String, val reference: Reference) : GeneratedValue {
            override fun expression(module: Module?): Expression = (generatorConstructor(name, listOf(Field(emptyList(), FieldIdentifier("value"), reference)), module).namedArguments.values.single() as FunctionCall)
                .let { it.copy(arguments = it.arguments + (Name.of("path") to VariableReference(Name.of("path")))) }
        }

        companion object {
            fun functionName(name: String): String = "generate$name"

            fun call(name: String): String = "${functionName(name)}(generator, emptyList())"
        }
    }

    /** A [record] outcome, or a list of records, takes that record's builder. */
    data class Outcome(
        val type: String,
        val wrapper: String?,
        val generated: GeneratedValue.Value?,
        val record: RecordRef?,
    )

    val generatedValues: List<GeneratedValue>
        get() = listOfNotNull(
            GeneratedValue.Request(requestName, parameters).takeIf { parameters.isNotEmpty() },
            result.generated,
            error?.generated,
        )

    companion object {
        fun from(
            rpc: Rpc,
            types: Map<String, Type> = emptyMap(),
            refined: Map<String, Refined> = emptyMap(),
        ): RpcShape {
            val name = Name.of(rpc.identifier.value).pascalCase()
            val requestName = "${name}Request"
            val parameters = rpc.shape.fields
            val requestFields = EndpointShape.extractBodyFields(requestName, types + (requestName to syntheticType(requestName, parameters)), refined)
            val result = outcome(name, "Result", rpc.result, wrapped = rpc.error != null, types)
            val references = parameters.map { it.reference } + listOfNotNull(rpc.result, rpc.error)

            return RpcShape(
                name = name,
                functionName = Name.of(rpc.identifier.value).camelCase().escapeKotlinIdentifier(),
                requestName = requestName,
                parameters = parameters,
                requestFields = requestFields,
                result = result,
                error = rpc.error?.let { outcome(name, "Error", it, wrapped = true, types) },
                callReturnType = if (rpc.error != null) "$name.Response" else result.type,
                modelImports = EndpointShape.modelImportsFor(references),
                generatorImports = references.flatMap(EndpointShape::collectCustomNames).distinct(),
            )
        }

        private fun outcome(
            rpcName: String,
            kind: String,
            reference: Reference,
            wrapped: Boolean,
            types: Map<String, Type>,
        ): Outcome {
            val valueType = KotlinGenerator.generateType(reference.convert())
            return Outcome(
                type = if (wrapped) "$rpcName.$kind" else valueType,
                wrapper = "$rpcName.$kind".takeIf { wrapped },
                generated = GeneratedValue.Value("$rpcName$kind", valueType, reference).takeIf { reference !is Reference.Unit },
                record = RecordRef.of(reference, types)?.takeIf { types.getValue(it.typeName).shape.fields.isNotEmpty() },
            )
        }
    }
}

private fun syntheticType(name: String, fields: List<Field>): Type = Type(
    comment = null,
    annotations = emptyList(),
    identifier = DefinitionIdentifier(name),
    shape = Shape(fields),
    extends = emptyList(),
)

private fun generatorConstructor(name: String, fields: List<Field>, module: Module?): ConstructorStatement = syntheticType(name, fields)
    .convertToGenerator(module)
    .findAll<IrFunction>()
    .single()
    .body
    .filterIsInstance<ReturnStatement>()
    .single()
    .expression as ConstructorStatement
