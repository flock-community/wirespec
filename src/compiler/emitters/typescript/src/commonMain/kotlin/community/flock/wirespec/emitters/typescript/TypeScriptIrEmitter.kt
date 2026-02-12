package community.flock.wirespec.emitters.typescript

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.IrEmitter
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
import community.flock.wirespec.language.converter.convert
import community.flock.wirespec.language.converter.requestParameters
import community.flock.wirespec.language.core.Element
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

    override fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> = super.emit(module, logger).let {
        it + Emitted("Wirespec", shared.source)
    }

    override fun emit(definition: Definition, module: Module, logger: Logger): Emitted =
        super.emit(definition, module, logger).let {
            val subPackageName = PackageName("") + definition
            val result = it.result.trimEnd('\n') + "\n"
            Emitted(
                file = subPackageName.toDir() + it.file.sanitizeSymbol(),
                result = "import {Wirespec} from '../Wirespec'\n\n$result"
            )
        }

    fun String.sanitizeSymbol() = asSequence()
        .filter { it.isLetterOrDigit() || it in listOf('_') }
        .joinToString("")

    fun Identifier.sanitizeSymbol() = value.sanitizeSymbol()

    override fun emit(type: AstType, module: Module): Emitted {
        val imports = type.importReferences().distinctBy { it.value }
            .joinToString("\n") { "import {${it.value}} from './${it.value}'" }
        val file = type.convert().restoreFieldNames(type.shape.value)
        return if (imports.isNotEmpty()) file.copy(elements = listOf(RawElement(imports)) + file.elements).toEmitted()
        else file.toEmitted()
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

    override fun emit(enum: AstEnum, module: Module): Emitted =
        enum.convert().toEmitted()

    override fun emit(union: Union): Emitted {
        val imports = union.importReferences().distinctBy { it.value }
            .joinToString("\n") { "import {type ${it.value}} from '../model'" }
        val file = union.convert()
        return if (imports.isNotEmpty()) file.copy(elements = listOf(RawElement(imports)) + file.elements).toEmitted()
        else file.toEmitted()
    }

    override fun emit(channel: Channel): Emitted {
        return channel.convert().toEmitted()
    }

    override fun emit(refined: Refined): Emitted {
        val converted = refined.convert()
        val validator = refined.emitValidator()
        return File(
            converted.name, listOf(
                RawElement("export type ${converted.name} = string;"),
                RawElement("export const validate${refined.identifier.value} = (value: ${refined.reference.convert()}):\n  $validator;"),
            )
        ).toEmitted()
    }

    fun Refined.emitValidator(): String {
        val defaultReturn = "true;"
        return when (val type = reference.type) {
            is Reference.Primitive.Type.Integer -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.Number -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.String -> type.constraint?.emit() ?: defaultReturn
            Reference.Primitive.Type.Boolean -> defaultReturn
            Reference.Primitive.Type.Bytes -> defaultReturn
        }
    }

    fun Reference.Primitive.Type.Constraint.emit() = when (this) {
        is Reference.Primitive.Type.Constraint.RegExp -> """$value.test(value)"""
        is Reference.Primitive.Type.Constraint.Bound -> """$min < value && value < $max;"""
    }

    override fun emit(endpoint: Endpoint): Emitted {
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
            .injectAfter { static: Static -> listOf(raw(api)) }

        return if (imports.isNotEmpty()) File(emit(endpoint.identifier), listOf(RawElement(imports), body)).toEmitted()
        else File(emit(endpoint.identifier), listOf(body)).toEmitted()
    }

    override fun emit(identifier: Identifier) = "\"${identifier.value}\""

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


    fun File.toEmitted(): Emitted = Emitted(name, generateTypeScript())


}
