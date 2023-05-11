package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.*
import community.flock.wirespec.compiler.core.parse.Shape.Field.Value.Custom
import community.flock.wirespec.compiler.core.parse.Shape.Field.Value.Primitive
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class KotlinEmitter(
    private val packageName: String = DEFAULT_PACKAGE_NAME,
    logger: Logger = noLogger
) : Emitter(logger) {

    override fun emit(ast: AST): List<Pair<String, String>> = super.emit(ast)
        .map { (name, result) -> name to if (packageName.isBlank()) "" else "package $packageName\n\n$result" }

    override fun TypeDefinition.emit() = withLogging(logger) {
        "data class ${name.emit()}(\n${shape.emit()}\n)\n\n"
    }

    override fun TypeDefinition.Name.emit() = withLogging(logger) { value }

    override fun Type.emit() = withLogging(logger) {
        when(this){
            is Shape -> this.emit()
            is Shape.Field.Value -> TODO()
        }
    }

    override fun Shape.emit() = withLogging(logger) {
        value.joinToString("\n") { it.emit() }.dropLast(1)
    }

    override fun Shape.Field.emit() = withLogging(logger) {
        "${SPACER}val ${key.emit()}: ${value.emit()}${if (isNullable) "?" else ""},"
    }

    override fun Shape.Field.Key.emit() = withLogging(logger) { value }

    override fun Shape.Field.Value.emit() = withLogging(logger) {
        when (this) {
            is Custom -> value
            is Primitive -> when (value) {
                Primitive.PrimitiveType.String -> "String"
                Primitive.PrimitiveType.Integer -> "Int"
                Primitive.PrimitiveType.Boolean -> "Boolean"
            }
        }.let { if (isIterable) "List<$it>" else it }
    }

    override fun EndpointDefinition.emit(): String {
        val params = this.path.filterIsInstance<EndpointDefinition.Segment.Param>()
            .map {
                when (it.type) {
                    is Shape -> TODO()
                    is Custom -> it.key to it.type.value
                    is Primitive -> it.key to it.type.value.name
                }
            }
            .joinToString(", ") { (k, v) -> "$k: $v" }
        val className = this.name.value
        return "interface ${this.name.value} {\n" +
                "\tsealed interface ${className}Response\n" +
                this.responses.map { it.emit(className) }.joinToString("\n") + "\n" +
                "\tfun ${this.name.value}($params):${this.name.value}Response\n" +
                "}\n\n"
    }

    override fun EndpointDefinition.Response.emit(className: String) =
        "\tdata class ${className}Response${
            contentType.replace(
                "/",
                ""
            )
        }${status}(val status:Int, val contentType:String, val content: ${type.toName()}): ${className}Response"

}
