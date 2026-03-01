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
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Parameter
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.Type as LanguageType
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.findElement
import community.flock.wirespec.ir.core.raw
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.core.transformer
import community.flock.wirespec.ir.generator.TypeScriptGenerator
import community.flock.wirespec.ir.generator.generateTypeScript
import community.flock.wirespec.compiler.core.parse.ast.Enum as AstEnum
import community.flock.wirespec.compiler.core.parse.ast.Shared as AstShared
import community.flock.wirespec.compiler.core.parse.ast.Type as AstType

open class TypeScriptIrEmitter : IrEmitter {

    override val generator = TypeScriptGenerator

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

    override fun emit(module: Module, logger: Logger): NonEmptyList<File> = super.emit(module, logger).let {
        it + File(Name.of("Wirespec"), listOf(RawElement(shared.source)))
    }

    override fun emit(definition: Definition, module: Module, logger: Logger): File =
        super.emit(definition, module, logger).let { file ->
            val subPackageName = PackageName("") + definition
            File(
                name = Name.of(subPackageName.toDir() + file.name.pascalCase().sanitizeSymbol()),
                elements = listOf(RawElement("import {Wirespec} from '../Wirespec'\n")) + file.elements
            )
        }

    fun String.sanitizeSymbol() = filter { it.isLetterOrDigit() || it == '_' }

    fun Identifier.sanitizeSymbol() = value.sanitizeSymbol()

    override fun emit(type: AstType, module: Module): File {
        val fieldValidations = type.classifyValidatableFields(module)
        val typeImports = type.importReferences().distinctBy { it.value }
            .joinToString("\n") { "import {${it.value}} from './${it.value}'" }
        val validateImports = fieldValidations.map { it.typeName }.distinct()
            .joinToString("\n") { "import {validate$it} from './$it'" }
        val allImports = listOf(typeImports, validateImports).filter { it.isNotEmpty() }.joinToString("\n")
        val fieldNames = type.shape.value.map { it.identifier.value }.toSet()
        // Transform validate body for TypeScript:
        // 1. Add obj receiver to bare FieldCalls that reference type fields
        // 2. Convert method-style validate calls to standalone function calls: x.validate() -> validateFoo(x)
        val tsTransformer = transformer {
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
        val file = type.convertWithValidation(module)
            .transform {
                matchingElements { fn: community.flock.wirespec.ir.core.Function ->
                    if (fn.name == Name.of("validate")) {
                        val validateName = "validate${type.identifier.value}"
                        val transformedBody = fn.body.map { tsTransformer.transformStatement(it) }
                        fn.copy(
                            name = Name.of(validateName),
                            parameters = listOf(Parameter(Name.of("obj"), LanguageType.Custom(type.identifier.value))),
                            body = transformedBody,
                        )
                    } else fn
                }
            }
        return if (allImports.isNotEmpty()) file.copy(elements = listOf(RawElement(allImports)) + file.elements)
        else file
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

        val endpointNamespace = endpoint.convert().findElement<Namespace>()!!
        val body = endpointNamespace
            .transform { injectAfter { _: Namespace -> listOf(raw(api)) } }

        return if (imports.isNotEmpty()) File(Name.of(endpoint.identifier.sanitize()), listOf(RawElement(imports), body))
        else File(Name.of(endpoint.identifier.sanitize()), listOf(body))
    }

    private fun Identifier.sanitize() = "\"${value}\""
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
