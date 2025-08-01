package community.flock.wirespec.emitters.kotlin

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.concatGenerics
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.orNull
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.core.removeQuestionMark
import community.flock.wirespec.compiler.utils.Logger

open class KotlinEmitter(
    private val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared(),
) : Emitter() {

    val import = """
        |
        |import $DEFAULT_SHARED_PACKAGE_STRING.kotlin.Wirespec
        |import kotlin.reflect.typeOf
        |
    """.trimMargin()

    override val extension = FileExtension.Kotlin

    override val shared = KotlinShared

    override val singleLineComment = "//"

    override fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> =
        super.emit(module, logger).let {
            if (emitShared.value) it + Emitted(PackageName("${DEFAULT_SHARED_PACKAGE_STRING}.kotlin").toDir() + "Wirespec", shared.source)
            else it
        }

    override fun emit(definition: Definition, module: Module, logger: Logger): Emitted =
        super.emit(definition, module, logger).let {
            val subPackageName = packageName + definition
            Emitted(
                file = subPackageName.toDir() + it.file.sanitizeSymbol(),
                result = """
                    |package $subPackageName
                    |${if (module.needImports()) import else ""}
                    |${it.result}
                """.trimMargin().trimStart()
            )
        }

    override fun emit(type: Type, module: Module) =
        if (type.shape.value.isEmpty()) "data object ${emit(type.identifier)}"
        else """
            |data class ${emit(type.identifier)}(
            |${type.shape.emit()}
            |)${type.extends.run { if (isEmpty()) "" else " : ${joinToString(", ") { it.emit() }}" }}
            |
        """.trimMargin()

    override fun Type.Shape.emit() = value.joinToString("\n") { "${Spacer}val ${it.emit()}," }.dropLast(1)

    override fun Field.emit() = "${emit(identifier)}: ${reference.emit()}"

    override fun Reference.emit(): String = when (this) {
        is Reference.Dict -> "Map<String, ${reference.emit()}>"
        is Reference.Iterable -> "List<${reference.emit()}>"
        is Reference.Unit -> "Unit"
        is Reference.Any -> "Any"
        is Reference.Custom -> value
        is Reference.Primitive -> when (val t = type) {
            is Reference.Primitive.Type.String -> "String"
            is Reference.Primitive.Type.Integer -> when (t.precision) {
                Reference.Primitive.Type.Precision.P32 -> "Int"
                Reference.Primitive.Type.Precision.P64 -> "Long"
            }

            is Reference.Primitive.Type.Number -> when (t.precision) {
                Reference.Primitive.Type.Precision.P32 -> "Float"
                Reference.Primitive.Type.Precision.P64 -> "Double"
            }

            is Reference.Primitive.Type.Boolean -> "Boolean"
            is Reference.Primitive.Type.Bytes -> "ByteArray"
        }
    }.let { if (isNullable) "$it?" else it }

    override fun emit(identifier: Identifier) = when (identifier) {
        is DefinitionIdentifier -> identifier.value.sanitizeSymbol()
        is FieldIdentifier -> identifier.value.sanitizeSymbol().sanitizeKeywords()
    }

    override fun emit(refined: Refined) = """
        |data class ${refined.identifier.value.sanitizeSymbol()}(override val value: String): Wirespec.Refined {
        |${Spacer}override fun toString() = value
        |}
        |
        |fun ${refined.identifier.value}.validate() = ${refined.emitValidator()}
        |
    """.trimMargin()

    override fun Refined.emitValidator():String {
        val defaultReturn = "true"
        return when (val type = reference.type) {
            is Reference.Primitive.Type.Integer -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.Number -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.String -> type.constraint?.emit() ?: defaultReturn
            Reference.Primitive.Type.Boolean -> defaultReturn
            Reference.Primitive.Type.Bytes -> defaultReturn
        }
    }

    override fun Reference.Primitive.Type.Constraint.emit() = when(this){
        is Reference.Primitive.Type.Constraint.RegExp -> "Regex(\"\"\"$expression\"\"\").matches(value)"
        is Reference.Primitive.Type.Constraint.Bound -> """${Spacer}$min < record.value < $max;"""
    }

    override fun emit(enum: Enum, module: Module) = """
        |enum class ${enum.identifier.value.sanitizeSymbol()} (override val label: String): Wirespec.Enum {
        |${enum.entries.joinToString(",\n") { "${it.sanitizeEnum().sanitizeKeywords()}(\"$it\")" }.spacer()};
        |${Spacer}override fun toString(): String {
        |${Spacer(2)}return label
        |${Spacer}}
        |}
        |
    """.trimMargin()

    override fun emit(union: Union) = """
        |sealed interface ${emit(union.identifier)}
        |
    """.trimMargin()

    override fun emit(channel: Channel) = """
        |${channel.emitImports()}
        |
        |interface ${emit(channel.identifier)}Channel {
        |   operator fun invoke(message: ${channel.reference.emit()})
        |}
        |
    """.trimMargin()

    private fun Definition.emitImports() = importReferences()
        .filter { identifier.value != it.value }
        .map { "import ${packageName.value}.model.${it.value};" }.joinToString("\n") { it.trimStart() }

    override fun emit(endpoint: Endpoint) = """
        |${endpoint.importReferences().map { "import ${packageName.value}.model.${it.value}" }.joinToString("\n") { it.trimStart() }}
        |
        |object ${emit(endpoint.identifier)} : Wirespec.Endpoint {
        |${endpoint.pathParams.emitObject("Path", "Wirespec.Path") { it.emit() }}
        |
        |${endpoint.queries.emitObject("Queries", "Wirespec.Queries") { it.emit() }}
        |
        |${endpoint.headers.emitObject("Headers", "Wirespec.Request.Headers") { it.emit() }}
        |
        |${endpoint.requests.first().emit(endpoint)}
        |
        |${Spacer}sealed interface Response<T: Any> : Wirespec.Response<T>
        |
        |${endpoint.emitStatusInterfaces()}
        |
        |${endpoint.emitResponseInterfaces()}
        |
        |${endpoint.responses.distinctByStatus().joinToString("\n\n") { it.emit() }}
        |
        |${Spacer}fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
        |${Spacer(2)}when(response) {
        |${endpoint.responses.distinctByStatus().joinToString("\n") { it.emitSerialized() }}
        |${Spacer(2)}}
        |
        |${Spacer}fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
        |${Spacer(2)}when (response.statusCode) {
        |${endpoint.responses.distinctByStatus().filter { it.status.isStatusCode() }.joinToString("\n") { it.emitDeserialized() }}
        |${Spacer(3)}else -> error("Cannot match response with status: ${'$'}{response.statusCode}")
        |${Spacer(2)}}
        |
        |${Spacer}interface Handler: Wirespec.Handler {
        |${Spacer(2)}${emitHandleFunction(endpoint)}
        |${Spacer(2)}companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
        |${Spacer(3)}override val pathTemplate = "/${endpoint.path.joinToString("/") { it.emit() }}"
        |${Spacer(3)}override val method = "${endpoint.method}"
        |${Spacer(3)}override fun server(serialization: Wirespec.Serialization<String>) = object : Wirespec.ServerEdge<Request, Response<*>> {
        |${Spacer(4)}override fun from(request: Wirespec.RawRequest) = fromRequest(serialization, request)
        |${Spacer(4)}override fun to(response: Response<*>) = toResponse(serialization, response)
        |${Spacer(3)}}
        |${Spacer(3)}override fun client(serialization: Wirespec.Serialization<String>) = object : Wirespec.ClientEdge<Request, Response<*>> {
        |${Spacer(4)}override fun to(request: Request) = toRequest(serialization, request)
        |${Spacer(4)}override fun from(response: Wirespec.RawResponse) = fromResponse(serialization, response)
        |${Spacer(3)}}
        |${Spacer(2)}}
        |${Spacer}}
        |}
        |
    """.trimMargin()

    open fun emitHandleFunction(endpoint: Endpoint): String =
        "suspend fun ${emit(endpoint.identifier).firstToLower()}(request: Request): Response<*>"

    private fun Endpoint.emitStatusInterfaces() = responses
        .map { it.status[0] }
        .distinct()
        .joinToString("\n") { "${Spacer}sealed interface Response${it}XX<T: Any> : Response<T>" }

    private fun Endpoint.emitResponseInterfaces() = responses
        .map { it.content.emit() }
        .distinct()
        .joinToString("\n") { "${Spacer}sealed interface Response${it.concatGenerics()} : Response<$it>" }

    private fun <E> List<E>.emitObject(name: String, extends: String, spaces: Int = 1, block: (E) -> String) =
        if (isEmpty()) "${Spacer(spaces)}data object $name : $extends"
        else """
            |${Spacer(spaces)}data class $name(
            |${joinToString(",\n") { "${Spacer(spaces + 1)}val ${block(it)}" }},
            |${Spacer(spaces)}) : $extends
        """.trimMargin()

    fun Endpoint.Request.emit(endpoint: Endpoint) = """
        |${Spacer}${emitConstructor(endpoint)}
        |${Spacer(2)}override val path = Path${endpoint.pathParams.joinToString { emit(it.identifier) }.brace()}
        |${Spacer(2)}override val method = Wirespec.Method.${endpoint.method.name}
        |${Spacer(2)}override val queries = Queries${endpoint.queries.joinToString { emit(it.identifier) }.brace()}
        |${Spacer(2)}override val headers = Headers${endpoint.headers.joinToString { emit(it.identifier) }.brace()}${if (content == null) "\n${Spacer(2)}override val body = Unit" else ""}
        |${Spacer}}
        |
        |${Spacer}fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
        |${Spacer(2)}Wirespec.RawRequest(
        |${Spacer(3)}path = listOf(${endpoint.path.joinToString { when (it) {is Endpoint.Segment.Literal -> """"${it.value}""""; is Endpoint.Segment.Param -> it.emitIdentifier() } }}),
        |${Spacer(3)}method = request.method.name,
        |${Spacer(3)}queries = ${if (endpoint.queries.isNotEmpty()) endpoint.queries.joinToString(" + ") { "(${it.emitSerializedParams("request", "queries")})" } else EMPTY_MAP},
        |${Spacer(3)}headers = ${if (endpoint.headers.isNotEmpty()) endpoint.headers.joinToString(" + ") { "(${it.emitSerializedParams("request", "headers")})" } else EMPTY_MAP},
        |${Spacer(3)}body = serialization.serialize(request.body, typeOf<${content.emit()}>()),
        |${Spacer(2)})
        |
        |${Spacer}fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
        |${Spacer(2)}Request${emitDeserializedParams(endpoint)}
    """.trimMargin()

    fun Endpoint.Response.emit() = """
        |${Spacer}data class Response$status(override val body: ${content.emit()}${headers.joinToString(", ") { "val ${it.emit()}" }.let { if (it.isBlank()) "" else ", $it"}}) : Response${status[0]}XX<${content.emit()}>, Response${content.emit().concatGenerics()} {
        |${Spacer(2)}override val status = ${status.fixStatus()}
        |${Spacer(2)}override val headers = ResponseHeaders${headers.joinToString { emit(it.identifier) }.brace()}
        |${headers.emitObject("ResponseHeaders", "Wirespec.Response.Headers", 2) { it.emit() }}
        |${Spacer}}
    """.trimMargin()

    private fun Endpoint.Request.emitConstructor(endpoint: Endpoint) = listOfNotNull(
        endpoint.pathParams.joinToString { Spacer(2) + it.emit() }.orNull(),
        endpoint.queries.joinToString { Spacer(2) + it.emit() }.orNull(),
        endpoint.headers.joinToString { Spacer(2) + it.emit() }.orNull(),
        content?.let { "${Spacer(2)}override val body: ${it.emit()}," }
    ).joinToString(",\n")
        .let { if (it.isBlank()) "object Request : Wirespec.Request<${content.emit()}> {" else "class Request(\n$it\n${Spacer}) : Wirespec.Request<${content.emit()}> {" }

    private fun Endpoint.Request.emitDeserializedParams(endpoint: Endpoint) = listOfNotNull(
        endpoint.indexedPathParams.joinToString { it.emitDeserialized() }.orNull(),
        endpoint.queries.joinToString { it.emitDeserializedParams("request", "queries") }.orNull(),
        endpoint.headers.joinToString { it.emitDeserializedParams("request", "headers") }.orNull(),
        content?.let { """${Spacer(3)}body = serialization.deserialize(requireNotNull(request.body) { "body is null" }, typeOf<${it.emit()}>()),""" }
    ).joinToString(",\n").let { if (it.isBlank()) "" else "(\n$it\n${Spacer(2)})" }

    private fun Endpoint.Response.emitSerialized() = """
        |${Spacer(3)}is Response$status -> Wirespec.RawResponse(
        |${Spacer(4)}statusCode = response.status,
        |${Spacer(4)}headers = ${if (headers.isNotEmpty()) headers.joinToString(" + ") { "(${it.emitSerializedParams("response", "headers")})" } else EMPTY_MAP},
        |${Spacer(4)}body = ${if (content != null) "serialization.serialize(response.body, typeOf<${content.emit()}>())" else "null"},
        |${Spacer(3)})
    """.trimMargin()

    private fun Endpoint.Response.emitDeserialized() = listOfNotNull(
        "${Spacer(3)}$status -> Response$status(",
        if (content != null) {
            "${Spacer(4)}body = serialization.deserialize(requireNotNull(response.body) { \"body is null\" }, typeOf<${content.emit()}>()),"
        } else {
            "${Spacer(4)}body = Unit,"
        },
        headers.joinToString(",\n") { it.emitDeserializedParams("response", "headers", 4) }.orNull(),
        "${Spacer(3)})"
    ).joinToString("\n")

    private fun Field.emitSerializedParams(type: String, fields: String) =
        """mapOf("${identifier.value}" to ($type.$fields.${emit(identifier)}?.let{ serialization.serializeParam(it, typeOf<${reference.emit()}>()) } ?: emptyList()))"""

    private fun IndexedValue<Endpoint.Segment.Param>.emitDeserialized() =
        """${Spacer(3)}${emit(value.identifier)} = serialization.deserialize(request.path[${index}], typeOf<${value.reference.emit()}>())"""

    private fun Field.emitDeserializedParams(type: String, fields: String, spaces: Int = 3) =
        if (reference.isNullable)
            """${Spacer(spaces)}${emit(identifier)} = $type.$fields["${identifier.value}"]?.let{ serialization.deserializeParam(it, typeOf<${reference.emit()}>()) }"""
        else
            """${Spacer(spaces)}${emit(identifier)} = serialization.deserializeParam(requireNotNull($type.$fields["${identifier.value}"]) { "${emit(identifier)} is null" }, typeOf<${reference.emit()}>())"""

    private fun Endpoint.Segment.Param.emitIdentifier() =
        "request.path.${emit(identifier)}.let{serialization.serialize(it, typeOf<${reference.emit()}>())}"

    private fun Endpoint.Content?.emit() = this?.reference?.emit()?.removeQuestionMark() ?: "Unit"

    private fun Endpoint.Segment.Param.emit() = "${emit(identifier)}: ${reference.emit()}"

    private fun String.brace() = wrap("(", ")")
    private fun String.wrap(prefix: String, postfix: String) = if (isEmpty()) "" else "$prefix$this$postfix"

    private fun String.sanitizeSymbol() = this
        .split(".", " ")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .asSequence()
        .filter { it.isLetterOrDigit() || it in listOf('_') }
        .joinToString("")
        .sanitizeFirstIsDigit()

    private fun String.sanitizeFirstIsDigit() = if (firstOrNull()?.isDigit() == true) "_${this}" else this

    fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//").joinToString("_").sanitizeFirstIsDigit()

    fun String.sanitizeKeywords() = if (this in reservedKeywords) addBackticks() else this

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "as", "break", "class", "continue", "do",
            "else", "false", "for", "fun", "if",
            "in", "interface", "internal", "is", "null",
            "object", "open", "package", "return", "super",
            "this", "throw", "true", "try", "typealias",
            "typeof", "val", "var", "when", "while", "private", "public"
        )

        private const val EMPTY_MAP = "emptyMap()"
    }
}
