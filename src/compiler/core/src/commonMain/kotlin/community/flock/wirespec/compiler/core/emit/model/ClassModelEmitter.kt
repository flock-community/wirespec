package community.flock.wirespec.compiler.core.emit.model

import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.transformer.ClassModelTransformer.transform
import community.flock.wirespec.compiler.core.emit.transformer.EndpointClass
import community.flock.wirespec.compiler.core.emit.transformer.EnumClass
import community.flock.wirespec.compiler.core.emit.transformer.Field
import community.flock.wirespec.compiler.core.emit.transformer.Parameter
import community.flock.wirespec.compiler.core.emit.transformer.Reference
import community.flock.wirespec.compiler.core.emit.transformer.RefinedClass
import community.flock.wirespec.compiler.core.emit.transformer.TypeClass
import community.flock.wirespec.compiler.core.emit.transformer.UnionClass
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.utils.Logger

abstract class ClassModelEmitter(logger: Logger, split: Boolean) : Emitter(logger, split) {

    override fun emit(ast: AST): List<Emitted> = ast
        .transform()
        .map {
            logger.log("Emitting Node $this")
            when (it) {
                is EndpointClass -> Emitted(it.name, it.emit())
                is EnumClass -> Emitted(it.name, it.emit())
                is RefinedClass -> Emitted(it.name, it.emit())
                is TypeClass -> Emitted(it.name, it.emit())
                is UnionClass -> Emitted(it.name, it.emit())
            }
        }
        .run {
            if (split) this
            else listOf(Emitted("NoName", joinToString("\n") { it.result }))
        }

    abstract fun TypeClass.emit(): String
    abstract fun RefinedClass.emit(): String
    abstract fun RefinedClass.Validator.emit(): String
    abstract fun EnumClass.emit(): String
    abstract fun UnionClass.emit(): String
    abstract fun EndpointClass.emit(): String
    abstract fun EndpointClass.RequestClass.emit(): String
    abstract fun EndpointClass.RequestClass.RequestAllArgsConstructor.emit(): String
    abstract fun EndpointClass.RequestClass.RequestParameterConstructor.emit(): String
    abstract fun EndpointClass.RequestMapper.emit(): String
    abstract fun EndpointClass.RequestMapper.RequestCondition.emit(): String
    abstract fun EndpointClass.ResponseInterface.emit(): String
    abstract fun EndpointClass.ResponseClass.emit(): String
    abstract fun EndpointClass.ResponseClass.ResponseAllArgsConstructor.emit(): String
    abstract fun EndpointClass.ResponseClass.ResponseParameterConstructor.emit(): String
    abstract fun EndpointClass.ResponseMapper.emit(): String
    abstract fun EndpointClass.ResponseMapper.ResponseCondition.emit(): String

    abstract fun Parameter.emit(): String
    abstract fun Reference.Generics.emit(): String
    abstract fun Reference.Custom.emit(): String
    abstract fun Reference.Language.emit(): String
    abstract fun Reference.Language.Primitive.emit(): String
    abstract fun Field.emit(): String
    abstract fun EndpointClass.Path.emit(): String
    abstract fun EndpointClass.Content.emit(): String
    abstract fun Reference.emit(): String
    abstract fun Reference.Wirespec.emit(): String

    fun String.spacer(space: Int = 1) = this
        .split("\n")
        .joinToString("\n") {
            if (it.isNotBlank()) {
                "${(1..space).joinToString("") { SPACER }}$it"
            } else {
                it
            }
        }

}