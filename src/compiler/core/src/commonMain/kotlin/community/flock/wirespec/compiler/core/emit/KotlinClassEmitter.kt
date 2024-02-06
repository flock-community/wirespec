package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.AbstractEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.common.ClassModelEmitter
import community.flock.wirespec.compiler.core.emit.common.ClassModelEmitter.Companion.SPACER
import community.flock.wirespec.compiler.core.parse.nodes.EndpointClass
import community.flock.wirespec.compiler.core.parse.nodes.EnumClass
import community.flock.wirespec.compiler.core.parse.nodes.Field
import community.flock.wirespec.compiler.core.parse.nodes.Parameter
import community.flock.wirespec.compiler.core.parse.nodes.Reference
import community.flock.wirespec.compiler.core.parse.nodes.RefinedClass
import community.flock.wirespec.compiler.core.parse.nodes.TypeClass

class KotlinClassEmitter : ClassModelEmitter {
    override fun TypeClass.emit(): String = """
        |data class $name(
        |${fields.joinToString(",\n") { it.emit() }.spacer()}
        |)
    """.trimMargin()

    override fun RefinedClass.emit() = """
        |data class ${name.sanitizeSymbol()}(override val value: String): Wirespec.Refined
        |fun $name.validate() = ${validator.emit()}
    """.trimMargin()

    override fun RefinedClass.Validator.emit() = "Regex($value).matches(value)"
    override fun EnumClass.emit(): String {
        fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//").joinToString("_").sanitizeFirstIsDigit()
        return """
            |enum class ${name.sanitizeSymbol()} (val label: String): Wirespec.Enum {
            |${entries.joinToString(",\n") { "${it.sanitizeEnum().sanitizeKeywords()}(\"$it\")" }.spacer()};
            |${SPACER}override fun toString(): String {
            |${SPACER}${SPACER}return label
            |${SPACER}}
            |}
        """.trimMargin()
    }

    override fun EndpointClass.emit(): String = """
        |interface $name : ${supers.joinToString(", ") { it.emit() }} {
        |${SPACER}sealed interface Request<T> : Wirespec.Request<T>
        |${requestClasses.joinToString("\n") { it.emit() }.spacer()}
        |
        |${SPACER}sealed interface Response<T> : Wirespec.Response<T>
        |${responseInterfaces.joinToString("\n") { it.emit() }.spacer()}
        |${responseClasses.joinToString("\n") { it.emit() }.spacer()}
        |${SPACER}companion object {
        |${SPACER}${SPACER}const val PATH = "$path"
        |${SPACER}${SPACER}const val METHOD = "$method"
        |${requestMapper.emit().spacer(2)}
        |${responseMapper.emit().spacer(2)}
        |  }
        |}
    """.trimMargin()

    override fun EndpointClass.RequestClass.emit() = """
         |data class ${name}(
         |${fields.joinToString(",\n") { it.emit() }.spacer()}
         |) : ${supers.joinToString(", ") { it.emit() }} {
         |${secondaryConstructor.emit().spacer()}
         |}
    """.trimMargin()

    override fun EndpointClass.RequestClass.PrimaryConstructor.emit(): String {
        throw NotImplementedError("primary constructor not needed for data class")
    }

    override fun EndpointClass.RequestClass.SecondaryConstructor.emit(): String = """
        |constructor(${parameters.joinToString(", ") { it.emit() }}) : this(
        |${SPACER}path = "${path.emit()}",
        |${SPACER}method = Wirespec.Method.${method},
        |${SPACER}query = mapOf<String, List<Any?>>(${query}),
        |${SPACER}headers = mapOf<String, List<Any?>>(${headers}),
        |${SPACER}content = ${content?.emit() ?: "null"}
        |)
    """.trimMargin()

    override fun EndpointClass.ResponseInterface.emit(): String = """
        |sealed interface ${name.emit()} : ${`super`.emit()}
    """.trimMargin()

    override fun EndpointClass.ResponseClass.emit(): String = """
        |data class ${name}(${fields.joinToString(", ") { it.emit() }}) : ${returnReference.emit()} {
        |${allArgsConstructor.emit().spacer()}
        |}
    """.trimMargin()

    override fun EndpointClass.ResponseClass.AllArgsConstructor.emit(): String = """
         |constructor(${this.parameters.joinToString(", ") { it.emit() }}): this(
         |${SPACER}status = $statusCode,
         |${SPACER}headers = headers,
         |${SPACER}content = ${content?.emit() ?: "null"},
         |)
    """.trimMargin()

    override fun EndpointClass.Path.emit(): String =
        value.joinToString("/", "/") {
            when (it) {
                is EndpointClass.Path.Literal -> it.value
                is EndpointClass.Path.Parameter -> "${'$'}{${it.value}}"
            }
        }

    override fun EndpointClass.Content.emit(): String = """
        |Wirespec.Content("$type", body)
    """.trimMargin()

    override fun EndpointClass.ResponseMapper.emit(): String = """
        |fun <B> $name(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
         |${SPACER}when {
         |${this.conditions.joinToString("\n") { it.emit() }.spacer(2)}
         |${SPACER}${SPACER}else -> error("Cannot map response with status ${'$'}{response.status}")
         |${SPACER}}
         |}
    """.trimMargin()

    override fun EndpointClass.ResponseMapper.ResponseCondition.emit(): String =
        if (content != null)
            """
                |response.status == $statusCode && response.content?.type == "${content.type}" -> contentMapper
                |  .read<Pet>(response.content!!, Wirespec.getType(Pet::class.java, false))
                |  .let { ${responseReference.emit()}(response.headers, it.body) }
            """.trimMargin()
        else
            """
                |response.status == $statusCode && response.content == null -> ${responseReference.emit()}(response.headers)
            """.trimMargin()


    override fun EndpointClass.RequestMapper.emit(): String = """
        |fun <B> $name(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
         |${SPACER}when {
         |${this.conditions.joinToString("\n") { it.emit() }.spacer(2)}
         |${SPACER}${SPACER}else -> error("Cannot map request")
         |${SPACER}}
         |}
    """.trimMargin()

    override fun EndpointClass.RequestMapper.RequestCondition.emit(): String =
        if (content != null)
            """
                |request.content?.type == "${content.type}" -> contentMapper
                |  .read<Pet>(request.content!!, Wirespec.getType(${content.reference.emit()}::class.java, ${isIterable}))
                |  .let { ${responseReference.emit()}(request.path, request.method, request.query, request.headers, it) }
            """.trimMargin()
        else
            """
                |request.content == null -> ${responseReference.emit()}(request.path, request.method, request.query, request.headers)
            """.trimMargin()

    override fun Parameter.emit(): String = """
        |${identifier}: ${reference.emit()}
    """.trimMargin()

    override fun Reference.Generics.emit(): String = """
        |${if (references.isNotEmpty()) references.joinToString(", ", "<", ">") { it.emit() } else ""}
    """.trimMargin()

    fun Reference.emit(): String = when (this) {
        is Reference.Custom -> emit()
        is Reference.Language -> emit()
    }
        .let { if (isOptional) "$it?" else it }
        .let { if (isIterable) "List<$it>" else it }

    override fun Reference.Custom.emit(): String = """
        |${name}${generics.emit()}${if (isNullable) "?" else ""}
    """.trimMargin()

    override fun Reference.Language.emit(): String = """
        |${primitive.emit()}${generics.emit()}${if (isNullable) "?" else ""}
    """.trimMargin()

    override fun Reference.Language.Primitive.emit(): String = when (this) {
        Reference.Language.Primitive.Any -> "Any"
        Reference.Language.Primitive.Unit -> "Unit"
        Reference.Language.Primitive.String -> "String"
        Reference.Language.Primitive.Integer -> "Int"
        Reference.Language.Primitive.Long -> "Long"
        Reference.Language.Primitive.Number -> "Double"
        Reference.Language.Primitive.Boolean -> "Boolean"
        Reference.Language.Primitive.Map -> "Map"
        Reference.Language.Primitive.List -> "List"
    }

    override fun Field.emit(): String = """
        |${if (isOverride) "override " else ""}val ${identifier}: ${reference.emit()}
    """.trimMargin()

    private fun String.sanitizeKeywords() = if (preservedKeywords.contains(this)) "`$this`" else this

    private fun String.sanitizeSymbol() = this
        .split(".", " ")
        .joinToString("") { it.firstToUpper() }
        .asSequence()
        .filter { it.isLetterOrDigit() || listOf('_').contains(it) }
        .joinToString("")
        .sanitizeFirstIsDigit()

    private fun String.sanitizeFirstIsDigit() = if (firstOrNull()?.isDigit() == true) "_${this}" else this

    companion object {
        private val preservedKeywords = listOf(
            "as", "break", "class", "continue", "do",
            "else", "false", "for", "fun", "if",
            "in", "interface", "internal", "is", "null",
            "object", "open", "package", "return", "super",
            "this", "throw", "true", "try", "typealias",
            "typeof", "val", "var", "when", "while"
        )
    }

}