package community.flock.wirespec.emitters.typescript

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.language.emit.IrEmitter
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
import community.flock.wirespec.language.converter.classifyValidatableFields
import community.flock.wirespec.language.converter.convert
import community.flock.wirespec.language.converter.convertConstraint
import community.flock.wirespec.language.converter.convertWithValidation
import community.flock.wirespec.language.converter.requestParameters
import community.flock.wirespec.language.core.Element
import community.flock.wirespec.language.core.Expression
import community.flock.wirespec.language.core.FieldCall
import community.flock.wirespec.language.core.FunctionCall
import community.flock.wirespec.language.core.Parameter
import community.flock.wirespec.language.core.Statement
import community.flock.wirespec.language.core.Struct
import community.flock.wirespec.language.core.VariableReference
import community.flock.wirespec.language.core.transformMatchingElements
import community.flock.wirespec.language.core.function
import community.flock.wirespec.language.core.Type as LanguageType
import community.flock.wirespec.language.core.File
import community.flock.wirespec.language.core.Interface
import community.flock.wirespec.language.core.RawElement
import community.flock.wirespec.language.core.Static
import community.flock.wirespec.language.core.file
import community.flock.wirespec.language.core.findElement
import community.flock.wirespec.language.core.injectAfter
import community.flock.wirespec.language.core.injectBefore
import community.flock.wirespec.language.core.raw
import community.flock.wirespec.language.core.transform
import community.flock.wirespec.language.core.transformChildren
import community.flock.wirespec.language.core.transformer
import community.flock.wirespec.language.generator.generatePython
import community.flock.wirespec.language.generator.generateTypeScript
import community.flock.wirespec.compiler.core.parse.ast.Enum as AstEnum
import community.flock.wirespec.compiler.core.parse.ast.Field as AstField
import community.flock.wirespec.compiler.core.parse.ast.Shared as AstShared
import community.flock.wirespec.compiler.core.parse.ast.Type as AstType

open class TypeScriptIrEmitter : IrEmitter {

    override fun File.generate(): String = generateTypeScript()

    override val extension = FileExtension.TypeScript

    override val shared = object : Shared {
        val api = """
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
          |  client: Client<REQ, RES>;
          |  server: Server<REQ, RES>
          |}
        """.trimMargin()
        override val packageString = DEFAULT_SHARED_PACKAGE_STRING
        override val source = AstShared(packageString)
            .convert()
            .injectBefore { static: Static ->
                if (static.name == "Wirespec") listOf(RawElement("export type Type = string"))
                else emptyList()
            }
            .injectAfter { static: Static ->
                if (static.name == "Wirespec") listOf(RawElement(api))
                else emptyList()
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

    override fun emit(module: Module, logger: Logger): NonEmptyList<File> = super.emit(module, logger).let {
        it + File("Wirespec", listOf(RawElement(shared.source)))
    }

    override fun emit(definition: Definition, module: Module, logger: Logger): File =
        super.emit(definition, module, logger).let { file ->
            val subPackageName = PackageName("") + definition
            File(
                name = subPackageName.toDir() + file.name.sanitizeSymbol(),
                elements = listOf(RawElement("import {Wirespec} from '../Wirespec'\n")) + file.elements
            )
        }

    fun String.sanitizeSymbol() = asSequence()
        .filter { it.isLetterOrDigit() || it in listOf('_') }
        .joinToString("")

    fun Identifier.sanitizeSymbol() = value.sanitizeSymbol()

    override fun emit(type: AstType, module: Module): File {
        val fieldValidations = type.classifyValidatableFields(module)
        val typeImports = type.importReferences().distinctBy { it.value }
            .joinToString("\n") { "import {${it.value}} from './${it.value}'" }
        val validateImports = fieldValidations.map { it.typeName }.distinct()
            .joinToString("\n") { "import {validate$it} from './$it'" }
        val allImports = listOf(typeImports, validateImports).filter { it.isNotEmpty() }.joinToString("\n")
        val fieldNames = type.shape.value.map { field ->
            field.identifier.value
                .split(".", " ", "-")
                .mapIndexed { index, s -> if (index > 0) s.replaceFirstChar { it.uppercaseChar() } else s }
                .joinToString("")
                .replaceFirstChar { it.lowercaseChar() }
        }.toSet()
        // Transform validate body for TypeScript:
        // 1. Add obj receiver to bare FieldCalls that reference type fields
        // 2. Convert method-style validate calls to standalone function calls: x.validate() -> validateFoo(x)
        val tsTransformer = transformer(
            transformStatement = { s, t ->
                when {
                    s is FunctionCall && s.name == "validate" && s.receiver != null && s.typeArguments.isNotEmpty() -> {
                        val typeName = (s.typeArguments.first() as? LanguageType.Custom)?.name ?: ""
                        FunctionCall(name = "validate$typeName", arguments = mapOf("obj" to t.transformExpression(s.receiver!!)))
                    }
                    s is FieldCall && s.receiver == null && s.field in fieldNames ->
                        FieldCall(receiver = VariableReference("obj"), field = s.field)
                    else -> s.transformChildren(t)
                }
            },
            transformExpression = { e, t ->
                when {
                    e is FunctionCall && e.name == "validate" && e.receiver != null && e.typeArguments.isNotEmpty() -> {
                        val typeName = (e.typeArguments.first() as? LanguageType.Custom)?.name ?: ""
                        FunctionCall(name = "validate$typeName", arguments = mapOf("obj" to t.transformExpression(e.receiver!!)))
                    }
                    e is FieldCall && e.receiver == null && e.field in fieldNames ->
                        FieldCall(receiver = VariableReference("obj"), field = e.field)
                    else -> e.transformChildren(t)
                }
            },
        )
        val file = type.convertWithValidation(module)
            .restoreFieldNames(type.shape.value)
            .transformMatchingElements { fn: community.flock.wirespec.language.core.Function ->
                if (fn.name == "validate") {
                    val validateName = "validate${type.identifier.value}"
                    val transformedBody = fn.body.map { tsTransformer.transformStatement(it) }
                    fn.copy(
                        name = validateName,
                        parameters = listOf(Parameter("obj", LanguageType.Custom(type.identifier.value))),
                        body = transformedBody,
                    )
                } else fn
            }
        return if (allImports.isNotEmpty()) file.copy(elements = listOf(RawElement(allImports)) + file.elements)
        else file
    }

    private fun <T : Element> T.restoreFieldNames(originalFields: List<AstField>): T {
        val nameMap = buildMap {
            originalFields.forEach { field ->
                val sanitized = field.identifier.value
                    .split(".", " ", "-")
                    .mapIndexed { index, s -> if (index > 0) s.replaceFirstChar { it.uppercaseChar() } else s }
                    .joinToString("")
                    .replaceFirstChar { it.lowercaseChar() }
                put(sanitized, field.identifier.value)
            }
        }
        return transform(
            transformer(
                transformField = { field, t ->
                    val originalName = nameMap[field.name] ?: field.name
                    field.copy(name = originalName).transformChildren(t)
                },
            ),
        )
    }

    override fun emit(enum: AstEnum, module: Module): File =
        enum.convert()

    override fun emit(union: Union): File {
        val imports = union.importReferences().distinctBy { it.value }
            .joinToString("\n") { "import {type ${it.value}} from '../model'" }
        val file = union.convert()
        return if (imports.isNotEmpty()) file.copy(elements = listOf(RawElement(imports)) + file.elements)
        else file
    }

    override fun emit(channel: Channel): File {
        return channel.convert()
    }

    override fun emit(refined: Refined): File {
        val converted = refined.convert()
        val constraintExpr = refined.reference.convertConstraint(VariableReference("value"))
        val validatorStr = function("_constraint") {
            returnType(LanguageType.Boolean)
            returns(constraintExpr)
        }.generateTypeScript().lines()
            .first { it.trimStart().startsWith("return ") }
            .trimStart()
            .removePrefix("return ")
            .removeSuffix(";")
        return File(
            converted.name, listOf(
                RawElement("export type ${converted.name} = ${emitTypeScriptReference(refined.reference)};"),
                RawElement("export const validate${refined.identifier.value} = (value: ${emitTypeScriptReference(refined.reference)}) =>\n  $validatorStr;"),
            )
        )
    }

    override fun emit(endpoint: Endpoint): File {
        val imports = endpoint.importReferences().distinctBy { it.value }
            .joinToString("\n") { "import {type ${it.value}} from '../model'" }

        val apiName = endpoint.identifier.value.replaceFirstChar { it.lowercase() }
        val method = endpoint.method.name
        val pathString = endpoint.path.joinToString("/") {
            when (it) {
                is Endpoint.Segment.Literal -> it.value
                is Endpoint.Segment.Param -> "{${it.identifier.value}}"
            }
        }
        val api = """
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
            |  server,
            |  client
            |} as const
        """.trimMargin()

        val endpointInterface = endpoint.convert().findElement<Interface>()!!
        val body = endpointInterface
            .toStatic()
            .injectAfter { _: Static -> listOf(raw(api)) }

        return if (imports.isNotEmpty()) File(endpoint.identifier.sanitize(), listOf(RawElement(imports), body))
        else File(endpoint.identifier.sanitize(), listOf(body))
    }

    private fun Identifier.sanitize() = "\"${value}\""
    private fun Interface.toStatic(): Static = Static(
        name = name,
        elements = elements,
        extends = extends.firstOrNull(),
    )

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


}
