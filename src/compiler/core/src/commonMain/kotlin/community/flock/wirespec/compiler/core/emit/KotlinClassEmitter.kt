package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.ClassModelEmitter
import community.flock.wirespec.compiler.core.emit.common.ClassModelEmitter.Companion.SPACER
import community.flock.wirespec.compiler.core.parse.nodes.Constructor
import community.flock.wirespec.compiler.core.parse.nodes.EndpointClass
import community.flock.wirespec.compiler.core.parse.nodes.Field
import community.flock.wirespec.compiler.core.parse.nodes.Parameter
import community.flock.wirespec.compiler.core.parse.nodes.Reference
import community.flock.wirespec.compiler.core.parse.nodes.Statement

class KotlinClassEmitter : ClassModelEmitter {

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
         |${constructors.drop(1).joinToString("\n") { "${it.emit()}" }.spacer()}
         |}
    """.trimMargin()

    override fun EndpointClass.ResponseInterface.emit(): String = """
        |sealed interface ${name.emit()} : ${`super`.emit()}
    """.trimMargin()

    override fun EndpointClass.ResponseClass.emit(): String = """
        |data class ${name}(${fields.joinToString(", ") { it.emit() }}) : ${returnReference.emit()} {
        |  override val status = ${statusCode};
        |  override val content = ${content?.emit() ?: "null"}
        |}
    """.trimMargin()

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

    override fun EndpointClass.ResponseMapper.Condition.emit(): String =
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

    override fun EndpointClass.RequestMapper.Condition.emit(): String =
        if(content != null)
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

    override fun Reference.Custom.emit(): String = """
        |${name}${generics.emit()}${if (nullable) "?" else ""}
    """.trimMargin()

    override fun Reference.Language.emit(): String = """
        |${primitive.emit()}${generics.emit()}${if (nullable) "?" else ""}
    """.trimMargin()

    override fun Reference.Language.Primitive.emit(): String = when (this) {
        Reference.Language.Primitive.Any -> "Any"
        Reference.Language.Primitive.Unit -> "Unit"
        Reference.Language.Primitive.String -> "String"
        Reference.Language.Primitive.Integer -> "Long"
        Reference.Language.Primitive.Number -> "Double"
        Reference.Language.Primitive.Boolean -> "Boolean"
        Reference.Language.Primitive.Map -> "Map"
        Reference.Language.Primitive.List -> "List"
    }

    override fun Statement.AssignField.emit(): String = """
        |${value} = ${statement.emit()}
    """.trimMargin()

    override fun Statement.Variable.emit(): String = """
        |$value
    """.trimMargin()

    override fun Statement.Literal.emit(): String = """
        |"$value"
    """.trimMargin()

    override fun Statement.Initialize.emit(): String = """
        |${this.reference.emitInitialize()}(${parameters.joinToString(", ") { it }})
    """.trimMargin()

    private fun Reference.emitInitialize(): String =
        when (this) {
            is Reference.Custom -> name
            is Reference.Language -> when (this.primitive) {
                Reference.Language.Primitive.Map -> "mapOf${generics.emit()}"
                Reference.Language.Primitive.List -> "listOf${generics.emit()}"
                else -> primitive.emit()
            }
        }

    override fun Statement.Concat.emit(): String = values.joinToString("") {
        when (it) {
            is Statement.Literal -> it.value
            is Statement.Variable -> "${'$'}{$it}"
            else -> error("Cannot emit statement: $it")
        }
    }.let { "\"${it}\"" }


    override fun Field.emit(): String = """
        |${if (override) "override " else ""}val ${identifier}: ${reference.emit()}
    """.trimMargin()

    override fun Constructor.emit(): String = """
        |constructor(${this.fields.joinToString(", ") { it.emit() }}) : this(
        |${body.joinToString(",\n") { it.emit() }.spacer()}
        |)
    """.trimMargin()

}