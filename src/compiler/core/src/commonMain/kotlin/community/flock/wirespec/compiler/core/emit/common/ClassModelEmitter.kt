package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.emit.transformer.ClassReference
import community.flock.wirespec.compiler.core.emit.transformer.EndpointClass
import community.flock.wirespec.compiler.core.emit.transformer.EnumClass
import community.flock.wirespec.compiler.core.emit.transformer.FieldClass
import community.flock.wirespec.compiler.core.emit.transformer.Parameter
import community.flock.wirespec.compiler.core.emit.transformer.RefinedClass
import community.flock.wirespec.compiler.core.emit.transformer.TypeClass
import community.flock.wirespec.compiler.core.emit.transformer.UnionClass

interface ClassModelEmitter :
    TypeClassEmitter,
    EnumClassEmitter,
    RefinedClassEmitter,
    LegacyEndpointClassEmitter,
    UnionClassEmitter {

    fun Parameter.emit(): String

    fun ClassReference.Generics.emit(): String

    fun ClassReference.Custom.emit(): String

    fun ClassReference.Language.emit(): String

    fun ClassReference.Language.Primitive.emit(): String

    fun FieldClass.emit(): String

    fun ClassReference.emit(): String

    fun ClassReference.Wirespec.emit(): String
}

interface TypeClassEmitter {
    fun TypeClass.emit(): String
}

interface EnumClassEmitter {
    fun EnumClass.emit(): String
}

interface RefinedClassEmitter {
    fun RefinedClass.emit(): String

    fun RefinedClass.Validator.emit(): String
}

interface LegacyEndpointClassEmitter {
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

    fun EndpointClass.Path.emit(): String

    fun EndpointClass.Content.emit(): String
}

interface UnionClassEmitter {
    fun UnionClass.emit(): String
}
