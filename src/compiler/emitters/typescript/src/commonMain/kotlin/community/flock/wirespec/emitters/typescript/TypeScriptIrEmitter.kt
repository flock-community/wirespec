package community.flock.wirespec.emitters.typescript

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.ir.emit.IrEmitter
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.Shared
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.emit.namespace
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.converter.classifyValidatableFields
import community.flock.wirespec.ir.converter.convert
import community.flock.wirespec.ir.converter.convertConstraint
import community.flock.wirespec.ir.converter.convertWithValidation
import community.flock.wirespec.ir.converter.requestParameters
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.ir.core.Assignment
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.ErrorStatement
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Literal
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Parameter
import community.flock.wirespec.ir.core.Switch
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.Type as LanguageType
import community.flock.wirespec.ir.core.Case
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.Transformer
import community.flock.wirespec.ir.core.findElement
import community.flock.wirespec.ir.core.import
import community.flock.wirespec.ir.core.plus
import community.flock.wirespec.ir.core.raw
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.core.transformer
import community.flock.wirespec.ir.transformer.SanitizationConfig
import community.flock.wirespec.ir.transformer.sanitizeFieldName
import community.flock.wirespec.ir.transformer.sanitizeNames
import community.flock.wirespec.ir.generator.TypeScriptGenerator
import community.flock.wirespec.ir.generator.generateTypeScript
import community.flock.wirespec.compiler.core.parse.ast.Enum as AstEnum
import community.flock.wirespec.compiler.core.parse.ast.Shared as AstShared
import community.flock.wirespec.compiler.core.parse.ast.Type as AstType

open class TypeScriptIrEmitter : IrEmitter {

    override val generator = TypeScriptGenerator

    override val extension = FileExtension.TypeScript

    private val sanitizationConfig: SanitizationConfig by lazy {
        SanitizationConfig(
            reservedKeywords = reservedKeywords,
            escapeKeyword = { "_$it" },
            fieldNameCase = { name ->
                val sanitized = if (name.parts.size > 1) name.camelCase() else name.value().sanitizeSymbol()
                Name(listOf(sanitized))
            },
            parameterNameCase = { name ->
                val sanitized = if (name.parts.size > 1) name.camelCase() else name.value().sanitizeSymbol()
                Name(listOf(sanitized))
            },
            sanitizeSymbol = { it.filter { ch -> ch.isLetterOrDigit() || ch == '_' } },
            escapeFieldKeywords = false,
            extraStatementTransforms = { stmt, tr ->
                when (stmt) {
                    is VariableReference -> VariableReference(
                        name = sanitizationConfig.sanitizeFieldName(stmt.name),
                    )
                    is ConstructorStatement -> ConstructorStatement(
                        type = tr.transformType(stmt.type),
                        namedArguments = stmt.namedArguments.map { (key, value) ->
                            sanitizationConfig.sanitizeFieldName(key) to tr.transformExpression(value)
                        }.toMap(),
                    )
                    is Assignment -> Assignment(
                        name = sanitizationConfig.sanitizeFieldName(stmt.name),
                        value = tr.transformExpression(stmt.value),
                        isProperty = stmt.isProperty,
                    )
                    else -> stmt.transformChildren(tr)
                }
            },
        )
    }

    override fun transformTestFile(file: File): File = file.transform {
        apply(transformPatternSwitchToValueSwitch())
    }

    override val shared = object : Shared {
        val api = """
          |export type Literal = { kind: "Literal"; value: string }
          |export type Param = { kind: "Param"; name: string; type: string }
          |export type PathSegment = Literal | Param
          |export type Client<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => {
          |  to: (request: REQ) => RawRequest;
          |  from: (response: RawResponse) => RES
          |}
          |export type Server<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => {
          |  from: (request: RawRequest) => REQ;
          |  to: (response: RES) => RawResponse
          |}
          |export type Api<REQ extends Request<unknown>, RES extends Response<unknown>> = {
          |  name: string;
          |  method: Method,
          |  path: string,
          |  pathSegments: PathSegment[],
          |  client: Client<REQ, RES>;
          |  server: Server<REQ, RES>
          |}
        """.trimMargin()
        override val packageString = DEFAULT_SHARED_PACKAGE_STRING
        override val source = AstShared(packageString)
            .convert()
            .transform {
                injectBefore { namespace: Namespace ->
                    if (namespace.name == Name.of("Wirespec")) listOf(RawElement("export type Type = string"))
                    else emptyList()
                }
                injectAfter { namespace: Namespace ->
                    if (namespace.name == Name.of("Wirespec")) listOf(RawElement(api))
                    else emptyList()
                }
            }
            .generateTypeScript()
    }

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = super.emit(ast, logger)
        .plus(
            ast.modules
                .flatMap { it.statements }
                .groupBy { def -> def.namespace() }
                .map { (ns, defs) ->
                    Emitted(
                        "${ns}/index.${extension.value}",
                        defs.joinToString("\n") { "export {${it.identifier.value}} from './${it.identifier.value}'" }
                    )
                }
        )

    override fun emit(module: Module, logger: Logger): NonEmptyList<File> =
        super.emit(module, logger) + File(Name.of("Wirespec"), listOf(RawElement(shared.source)))

    override fun emit(definition: Definition, module: Module, logger: Logger): File {
        val file = super.emit(definition, module, logger)
        val subPackageName = PackageName("") + definition
        return File(
            name = Name.of(subPackageName.toDir() + file.name.pascalCase().sanitizeSymbol()),
            elements = listOf(import("../Wirespec", "Wirespec")) + file.elements
        )
    }

    override fun emit(type: AstType, module: Module): File {
        val fieldValidations = type.classifyValidatableFields(module)
        val typeImports = type.importReferences().distinctBy { it.value }
            .map { import("./${it.value}", it.value) }
        val validateImports = fieldValidations.map { it.typeName }.distinct()
            .filter { it != type.identifier.value }
            .map { import("./$it", "validate$it") }
        val allImports = typeImports + validateImports
        val fieldNames = type.shape.value.map { it.identifier.value }.toSet()
        val file = type.convertWithValidation(module)
            .sanitizeNames(sanitizationConfig)
            .transform {
                matchingElements { fn: community.flock.wirespec.ir.core.Function ->
                    if (fn.name == Name.of("validate")) {
                        fn.copy(
                            name = Name.of("validate${type.identifier.value}"),
                            parameters = listOf(Parameter(Name.of("obj"), LanguageType.Custom(type.identifier.value))),
                        ).transform {
                            statementAndExpression { s, t ->
                                when {
                                    s is FunctionCall && s.name == Name.of("validate") && s.receiver != null && s.typeArguments.isNotEmpty() -> {
                                        val typeName = (s.typeArguments.first() as? LanguageType.Custom)?.name ?: ""
                                        FunctionCall(name = Name.of("validate$typeName"), arguments = mapOf(Name.of("obj") to t.transformExpression(s.receiver!!)))
                                    }
                                    s is FieldCall && s.receiver == null && s.field.camelCase() in fieldNames ->
                                        FieldCall(receiver = VariableReference(Name.of("obj")), field = s.field)
                                    else -> s.transformChildren(t)
                                }
                            }
                        }
                    } else fn
                }
            }
        return if (allImports.isNotEmpty()) file.copy(elements = allImports + file.elements)
        else file
    }

    override fun emit(enum: AstEnum, module: Module): File =
        enum.convert()
            .sanitizeNames(sanitizationConfig)

    override fun emit(union: Union): File {
        val imports = union.importReferences().distinctBy { it.value }
            .map { import("../model", it.value, isTypeOnly = true) }
        val file = union.convert().sanitizeNames(sanitizationConfig)
        return if (imports.isNotEmpty()) file.copy(elements = imports + file.elements)
        else file
    }

    override fun emit(refined: Refined): File {
        val converted = refined.convert()
        val constraintExpr = refined.reference.convertConstraint(VariableReference(Name.of("value")))
        val validatorStr = TypeScriptGenerator.generateExpression(constraintExpr)
        return File(
            converted.name, listOf(
                RawElement("export type ${converted.name.pascalCase()} = ${emitTypeScriptReference(refined.reference)};"),
                RawElement("export const validate${refined.identifier.value} = (value: ${emitTypeScriptReference(refined.reference)}) =>\n  $validatorStr;"),
            )
        )
    }

    override fun emit(endpoint: Endpoint): File {
        val imports = endpoint.importReferences().distinctBy { it.value }
            .map { import("../model", it.value, isTypeOnly = true) }

        val hasRequestParams = endpoint.requestParameters().isNotEmpty()
        return endpoint.convert()
            .transform {
                statement { stmt, transformer ->
                    when (stmt) {
                        is Switch -> stmt.copy(
                            default = stmt.default?.map { s ->
                                if (s is ErrorStatement && s.message is BinaryOp) {
                                    val binary = s.message as BinaryOp
                                    val literal = binary.left as? Literal
                                    if (literal != null) ErrorStatement(Literal(literal.value.toString().trimEnd(' '), literal.type))
                                    else s
                                } else s
                            }
                        ).transformChildren(transformer)
                        else -> stmt.transformChildren(transformer)
                    }
                }
            }
            .transform { apply(transformPatternSwitchToValueSwitch()) }
            .transform {
                if (hasRequestParams) {
                    matchingElements { iface: community.flock.wirespec.ir.core.Interface ->
                        if (iface.name == Name.of("Call")) {
                            iface.copy(
                                elements = iface.elements.map { element ->
                                    if (element is community.flock.wirespec.ir.core.Function) {
                                        element.copy(
                                            parameters = listOf(
                                                Parameter(Name.of("params"), LanguageType.Custom("RequestParams"))
                                            )
                                        )
                                    } else element
                                }
                            )
                        } else iface
                    }
                }
            }
            .transform { injectAfter<Namespace>{listOf(buildApiConst(endpoint))}}
            .copy(name = Name.of(endpoint.identifier.sanitize()))
            .sanitizeNames(sanitizationConfig)
            .let {
                if (imports.isNotEmpty()) it.copy(elements = imports + it.elements)
                else it
            }

    }

    private fun buildApiConst(endpoint: Endpoint): RawElement {
        val apiName = endpoint.identifier.value.firstToLower()
        val method = endpoint.method.name
        val pathString = endpoint.path.joinToString("/") {
            when (it) {
                is Endpoint.Segment.Literal -> it.value
                is Endpoint.Segment.Param -> "{${it.identifier.value}}"
            }
        }
        val pathSegmentsCode = endpoint.path.joinToString(", ") { segment ->
            when (segment) {
                is Endpoint.Segment.Literal -> """{ kind: "Literal", value: "${segment.value}" }"""
                is Endpoint.Segment.Param -> """{ kind: "Param", name: "${segment.identifier.value}", type: "${segment.reference.toTypeScriptTypeName()}" }"""
            }
        }
        return raw("""
            |export const client:Wirespec.Client<Request, Response> = (serialization: Wirespec.Serialization) => ({
            |  from: (it) => fromRawResponse(serialization, it),
            |  to: (it) => toRawRequest(serialization, it)
            |})
            |export const server:Wirespec.Server<Request, Response> = (serialization: Wirespec.Serialization) => ({
            |  from: (it) => fromRawRequest(serialization, it),
            |  to: (it) => toRawResponse(serialization, it)
            |})
            |export const api = {
            |  name: "$apiName",
            |  method: "$method",
            |  path: "$pathString",
            |  pathSegments: [$pathSegmentsCode],
            |  server,
            |  client
            |} as const
        """.trimMargin())
    }

    private fun Reference.toTypeScriptTypeName(): String = when (this) {
        is Reference.Primitive -> when (type) {
            is Reference.Primitive.Type.String -> "string"
            is Reference.Primitive.Type.Integer -> "number"
            is Reference.Primitive.Type.Number -> "number"
            is Reference.Primitive.Type.Boolean -> "boolean"
            is Reference.Primitive.Type.Bytes -> "ArrayBuffer"
        }
        is Reference.Custom -> value
        is Reference.Iterable -> "${reference.toTypeScriptTypeName()}[]"
        is Reference.Dict -> "Record<string, ${reference.toTypeScriptTypeName()}>"
        is Reference.Any -> "any"
        is Reference.Unit -> "undefined"
    }

    override fun emit(channel: Channel): File =
        channel.convert()
            .sanitizeNames(sanitizationConfig)

    override fun emitEndpointClient(endpoint: Endpoint): File {
        val endpointName = endpoint.identifier.value
        val methodName = endpointName.firstToLower()

        val imports = endpoint.importReferences().distinctBy { it.value }
            .map { import("../model", it.value, isTypeOnly = true) }

        val params = buildEndpointParams(endpoint)
        val paramList = if (params.isNotEmpty()) "params: $endpointName.RequestParams" else ""
        val requestArgs = if (params.isNotEmpty()) "$endpointName.request(params)" else "$endpointName.request()"

        val code = """
            |export const ${methodName}Client = (serialization: Wirespec.Serialization, transportation: Wirespec.Transportation) => ({
            |  $methodName: async ($paramList): Promise<$endpointName.Response<unknown>> => {
            |    const request: $endpointName.Request = $requestArgs;
            |    const rawRequest = $endpointName.toRawRequest(serialization, request);
            |    const rawResponse = await transportation.transport(rawRequest);
            |    return $endpointName.fromRawResponse(serialization, rawResponse);
            |  }
            |})
        """.trimMargin()

        return File(
            Name.of("client/${endpointName}Client"),
            buildList {
                add(import("../Wirespec", "Wirespec"))
                add(import("../endpoint/$endpointName", endpointName))
                addAll(imports)
                add(RawElement(code))
            }
        )
    }

    override fun emitClient(endpoints: List<Endpoint>, logger: Logger): File {
        logger.info("Emitting main Client for ${endpoints.size} endpoints")

        val clientImports = endpoints.map {
            val methodName = it.identifier.value.firstToLower()
            import("./client/${it.identifier.value}Client", "${methodName}Client")
        }

        val spreadEntries = endpoints.joinToString("\n") {
            val methodName = it.identifier.value.firstToLower()
            "  ...${methodName}Client(serialization, transportation),"
        }

        val code = """
            |export const client = (serialization: Wirespec.Serialization, transportation: Wirespec.Transportation) => ({
            |$spreadEntries
            |})
        """.trimMargin()

        return File(
            Name.of("Client"),
            buildList {
                add(import("./Wirespec", "Wirespec"))
                addAll(clientImports)
                add(RawElement(code))
            }
        )
    }

    private fun transformPatternSwitchToValueSwitch(): Transformer = transformer {
        statement { stmt, tr ->
            if (stmt is Switch && stmt.cases.any { it.type != null }) {
                val varName = stmt.variable?.camelCase() ?: "r"
                val transformedCases = stmt.cases.map { case ->
                    val typeName = (case.type as? LanguageType.Custom)?.name
                    val statusNum = typeName
                        ?.substringAfterLast(".")
                        ?.removePrefix("Response")
                        ?.toIntOrNull()
                    if (statusNum != null && typeName != null) {
                        val exprCode = TypeScriptGenerator.generateExpression(tr.transformExpression(stmt.expression))
                        val castAssignment = Assignment(
                            name = Name.of(varName),
                            value = RawExpression("$exprCode as $typeName"),
                            isProperty = false,
                        )
                        Case(
                            value = Literal(statusNum, LanguageType.Integer()),
                            body = listOf(castAssignment) + case.body.map { tr.transformStatement(it) },
                            type = null,
                        )
                    } else {
                        case.copy(body = case.body.map { tr.transformStatement(it) })
                    }
                }
                Switch(
                    expression = FieldCall(
                        receiver = tr.transformExpression(stmt.expression),
                        field = Name.of("status"),
                    ),
                    cases = transformedCases,
                    default = stmt.default?.map { tr.transformStatement(it) },
                    variable = null,
                )
            } else stmt.transformChildren(tr)
        }
    }

    private fun Identifier.sanitize() = "\"${value}\""

    private fun String.sanitizeSymbol() = filter { it.isLetterOrDigit() || it == '_' }

    private fun String.sanitizeKeywords() = if (this in reservedKeywords) "_$this" else this

    private fun String.firstToLower() = replaceFirstChar { it.lowercase() }

    private fun sanitizeParamName(identifier: Identifier): String {
        val parts = identifier.value.split(Regex("[.\\s-]+")).filter { it.isNotEmpty() }
        val name = if (parts.size > 1) Name(parts).camelCase() else identifier.value
        return name.sanitizeSymbol().sanitizeKeywords()
    }

    private fun buildEndpointParams(endpoint: Endpoint): List<EndpointParam> = buildList {
        endpoint.path.filterIsInstance<Endpoint.Segment.Param>().forEach {
            add(EndpointParam(sanitizeParamName(it.identifier), emitTypeScriptReference(it.reference.copy(isNullable = false)), it.reference.isNullable))
        }
        endpoint.queries.forEach {
            add(EndpointParam(sanitizeParamName(it.identifier), emitTypeScriptReference(it.reference.copy(isNullable = false)), it.reference.isNullable))
        }
        endpoint.headers.forEach {
            add(EndpointParam(sanitizeParamName(it.identifier), emitTypeScriptReference(it.reference.copy(isNullable = false)), it.reference.isNullable))
        }
        endpoint.requests.first().content?.let {
            add(EndpointParam("body", emitTypeScriptReference(it.reference.copy(isNullable = false)), it.reference.isNullable))
        }
    }

    private fun emitTypeScriptReference(ref: Reference): String = when (ref) {
        is Reference.Dict -> "Record<string, ${emitTypeScriptReference(ref.reference)}>"
        is Reference.Iterable -> "${emitTypeScriptReference(ref.reference)}[]"
        is Reference.Unit -> "undefined"
        is Reference.Any -> "any"
        is Reference.Custom -> ref.value.sanitizeSymbol()
        is Reference.Primitive -> when (ref.type) {
            is Reference.Primitive.Type.String -> "string"
            is Reference.Primitive.Type.Integer -> "number"
            is Reference.Primitive.Type.Number -> "number"
            is Reference.Primitive.Type.Boolean -> "boolean"
            is Reference.Primitive.Type.Bytes -> "ArrayBuffer"
        }
    }.let { "$it${if (ref.isNullable) " | undefined" else ""}" }

    private data class EndpointParam(val name: String, val type: String, val nullable: Boolean)

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "break", "case", "catch", "continue", "debugger",
            "default", "delete", "do", "else", "finally",
            "for", "function", "if", "in", "instanceof",
            "new", "return", "switch", "this", "throw",
            "try", "typeof", "var", "void", "while",
            "with", "class", "const", "enum", "export",
            "extends", "import", "super", "implements",
            "interface", "let", "package", "private",
            "protected", "public", "static", "yield",
            "type", "async", "await",
        )
    }

}
