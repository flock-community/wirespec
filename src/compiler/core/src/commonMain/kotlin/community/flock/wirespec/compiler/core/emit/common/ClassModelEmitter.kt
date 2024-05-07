package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.emit.transformer.EndpointClass
import community.flock.wirespec.compiler.core.emit.transformer.EnumClass
import community.flock.wirespec.compiler.core.emit.transformer.FieldClass
import community.flock.wirespec.compiler.core.emit.transformer.Parameter
import community.flock.wirespec.compiler.core.emit.transformer.Reference
import community.flock.wirespec.compiler.core.emit.transformer.RefinedClass
import community.flock.wirespec.compiler.core.emit.transformer.TypeClass
import community.flock.wirespec.compiler.core.emit.transformer.UnionClass

interface ClassModelEmitter {

    fun TypeClass.emit(): String

    fun RefinedClass.emit(): String

    fun RefinedClass.Validator.emit(): String

    fun EnumClass.emit(): String

    fun UnionClass.emit(): String

    fun EndpointClass.emit(): String

    fun EndpointClass.RequestClass.emit(): String

    fun EndpointClass.RequestClass.RequestAllArgsConstructor.emit(): String

    fun EndpointClass.RequestClass.RequestParameterConstructor.emit(): String

    fun EndpointClass.RequestMapper.emit(): String

    fun EndpointClass.RequestMapper.RequestCondition.emit(): String

    fun EndpointClass.ResponseInterface.emit(): String

    fun EndpointClass.ResponseClass.emit(): String

    fun EndpointClass.ResponseClass.ResponseAllArgsConstructor.emit(): String

    fun EndpointClass.ResponseClass.ResponseParameterConstructor.emit(): String

    fun EndpointClass.ResponseMapper.emit(): String

    fun EndpointClass.ResponseMapper.ResponseCondition.emit(): String

    fun Parameter.emit(): String

    fun Reference.Generics.emit(): String

    fun Reference.Custom.emit(): String

    fun Reference.Language.emit(): String

    fun Reference.Language.Primitive.emit(): String

    fun FieldClass.emit(): String

    fun EndpointClass.Path.emit(): String

    fun EndpointClass.Content.emit(): String

    fun Reference.emit(): String

    fun Reference.Wirespec.emit(): String
}
