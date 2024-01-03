package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.AbstractEmitter
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Enum
import community.flock.wirespec.compiler.core.parse.nodes.Node
import community.flock.wirespec.compiler.core.parse.nodes.Refined
import community.flock.wirespec.compiler.core.parse.nodes.Type
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class WirespecEmitter(logger: Logger = noLogger) : AbstractEmitter(logger) {

    override val shared = ""

    override fun Type.emit() = withLogging(logger) {
        """type $name {
            |${shape.emit()}
            |}
            |""".trimMargin()
    }

    override fun Type.Shape.emit() = withLogging(logger) {
        value.joinToString(",\n") { "$SPACER${it.emit()}" }
    }

    override fun Type.Shape.Field.emit() = withLogging(logger) {
        "${identifier.emit()}: ${reference.emit()}${if (isNullable) "?" else ""}"
    }

    override fun Type.Shape.Field.Identifier.emit() = withLogging(logger) { value }

    override fun Type.Shape.Field.Reference.emit() = withLogging(logger) {
        when (this) {
            is Type.Shape.Field.Reference.Unit -> "Unit"
            is Type.Shape.Field.Reference.Any -> "Any"
            is Type.Shape.Field.Reference.Custom -> value
            is Type.Shape.Field.Reference.Primitive -> when (type) {
                Type.Shape.Field.Reference.Primitive.Type.String -> "String"
                Type.Shape.Field.Reference.Primitive.Type.Integer -> "Integer"
                Type.Shape.Field.Reference.Primitive.Type.Number -> "Number"
                Type.Shape.Field.Reference.Primitive.Type.Boolean -> "Boolean"
            }
        }.let { if (isIterable) "$it[]" else it }
    }

    override fun Enum.emit() = withLogging(logger) { "enum $name {\n${SPACER}${entries.joinToString(", ")}\n}\n" }

    override fun Refined.emit() = withLogging(logger) {
        "refined $name ${validator.emit()}\n"
    }

    override fun Refined.Validator.emit() = withLogging(logger) {
        "/${value.drop(1).dropLast(1)}/g"
    }

    override fun Endpoint.emit() = withLogging(logger) {
        """
          |endpoint ${name} ${method}${requests.emitRequest()} ${path.emitPath()}${query.emitQuery()} -> {
          |${responses.joinToString ("\n"){ "$SPACER${ it.status } -> ${it.content?.reference?.emit()}${if(it.content?.isNullable == true) "?" else ""}"}}
          |}
          |
        """.trimMargin()
    }

    override fun Node.emitName(): String = when(this){
        is Endpoint -> this.name
        is Enum -> this.name
        is Refined -> this.name
        is Type -> this.name
    }

    private fun List<Endpoint.Segment>.emitPath() = "/" + joinToString("/") {
        when (it) {
            is Endpoint.Segment.Param -> "{${it.identifier.value}: ${it.reference.emit()}}"
            is Endpoint.Segment.Literal -> it.value
        }
    }

    private fun List<Endpoint.Request>.emitRequest() = firstOrNull()?.content?.reference?.emit()?.let { " $it" }.orEmpty()

    private fun List<Type.Shape.Field>.emitQuery() = takeIf { it.isNotEmpty() }
        ?.joinToString (",", "{", "}"){ it.emit() }
        ?.let { " ? $it" }
        .orEmpty()

}
