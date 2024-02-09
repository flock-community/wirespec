package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.nodes.ClassModel
import community.flock.wirespec.compiler.core.parse.nodes.Definition
import community.flock.wirespec.compiler.core.parse.nodes.EndpointClass
import community.flock.wirespec.compiler.core.parse.nodes.EnumClass
import community.flock.wirespec.compiler.core.parse.nodes.Field
import community.flock.wirespec.compiler.core.parse.nodes.Parameter
import community.flock.wirespec.compiler.core.parse.nodes.Reference
import community.flock.wirespec.compiler.core.parse.nodes.RefinedClass
import community.flock.wirespec.compiler.core.parse.nodes.TypeClass
import community.flock.wirespec.compiler.core.parse.transformer.ClassModelTransformer
import community.flock.wirespec.compiler.utils.Logger

abstract class ClassModelEmitter(
    override val logger: Logger,
    override val split: Boolean
) : Emitter(logger, split) {

    companion object {
        const val SPACER = "  "
    }

    override fun emit(ast: List<Definition>): List<Emitted> =
        ClassModelTransformer.transform(ast)
            .map { Emitted(it.name, emit(it)) }
            .run {
                if (split) this
                else listOf(Emitted("NoName", joinToString("\n") { it.result }))
            }

    open fun emit(node: ClassModel): String =
        when (node) {
            is EndpointClass -> node.emit()
            is TypeClass -> node.emit()
            is RefinedClass -> node.emit()
            is EnumClass -> node.emit()
        }

    abstract fun TypeClass.emit(): String
    abstract fun RefinedClass.emit(): String
    abstract fun RefinedClass.Validator.emit(): String
    abstract fun EnumClass.emit(): String
    abstract fun EndpointClass.emit(): String
    abstract fun EndpointClass.RequestClass.emit(): String
    abstract fun EndpointClass.RequestClass.RequestAllArgsConstructor.emit(): String
    abstract fun EndpointClass.RequestClass.RequestParameterConstructor.emit(): String
    abstract fun EndpointClass.RequestMapper.emit(): String
    abstract fun EndpointClass.RequestMapper.RequestCondition.emit(): String
    abstract fun EndpointClass.ResponseInterface.emit(): String
    abstract fun EndpointClass.ResponseClass.emit(): String
    abstract fun EndpointClass.ResponseClass.ResponseAllArgsConstructor.emit(): String
    abstract fun EndpointClass.ResponseMapper.emit(): String
    abstract fun EndpointClass.ResponseMapper.ResponseCondition.emit(): String

    abstract fun Parameter.emit(): String
    abstract fun Reference.Generics.emit(): String
    abstract fun Reference.Custom.emit(): String
    abstract fun Reference.Language.emit(): String
    abstract fun Reference.Language.Primitive.emit(): String
    abstract fun Field.emit(): String
    fun String.spacer(space: Int = 1) = this
        .split("\n")
        .joinToString("\n") {
            if (it.isNotBlank()) {
                "${(1..space).joinToString("") { SPACER }}$it"
            } else {
                it
            }
        }

    abstract fun EndpointClass.Path.emit(): String
    abstract fun EndpointClass.Content.emit(): String
    abstract fun Reference.emit(): String
    abstract fun Reference.Wirespec.emit(): String
}