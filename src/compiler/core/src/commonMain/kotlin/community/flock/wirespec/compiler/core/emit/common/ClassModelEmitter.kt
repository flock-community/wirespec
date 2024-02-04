package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.nodes.ClassModel
import community.flock.wirespec.compiler.core.parse.nodes.EndpointClass
import community.flock.wirespec.compiler.core.parse.nodes.Field
import community.flock.wirespec.compiler.core.parse.nodes.Parameter
import community.flock.wirespec.compiler.core.parse.nodes.Reference

interface ClassModelEmitter {
    companion object {
        const val SPACER = "  "
    }

    fun emit(ast: List<ClassModel>): Map<String, String> = ast.associate {
        when (it) {
            is EndpointClass -> it.name to it.emit()
        }
    }

    fun EndpointClass.emit(): String
    fun EndpointClass.RequestClass.emit(): String
    fun EndpointClass.RequestClass.PrimaryConstructor.emit(): String
    fun EndpointClass.RequestClass.SecondaryConstructor.emit(): String
    fun EndpointClass.RequestMapper.emit(): String
    fun EndpointClass.RequestMapper.RequestCondition.emit(): String
    fun EndpointClass.ResponseInterface.emit(): String
    fun EndpointClass.ResponseClass.emit(): String
    fun EndpointClass.ResponseClass.AllArgsConstructor.emit(): String
    fun EndpointClass.ResponseMapper.emit(): String
    fun EndpointClass.ResponseMapper.ResponseCondition.emit(): String

    fun Parameter.emit(): String
    fun Reference.emit(): String = when (this) {
        is Reference.Custom -> emit()
        is Reference.Language -> emit()
    }

    fun Reference.Generics.emit(): String
    fun Reference.Custom.emit(): String
    fun Reference.Language.emit(): String
    fun Reference.Language.Primitive.emit(): String
    fun Field.emit(): String
    fun String.spacer(space: Int = 1) = this
        .split("\n")
        .joinToString("\n") {
            if (it.isNotBlank()) {
                "${(1..space).joinToString("") { SPACER }}$it"
            } else {
                it
            }
        }

    fun EndpointClass.Path.emit(): String
    fun EndpointClass.Content.emit(): String
}