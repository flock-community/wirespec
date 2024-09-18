package community.flock.wirespec.compiler.core.emit.transformer

import community.flock.wirespec.compiler.core.Value

sealed interface ClassModel {
    val name: String
}

data class TypeClass(
    override val name: String,
    val fields: List<FieldClass>,
    val supers: List<ClassReference> = emptyList(),
) : ClassModel

data class RefinedClass(
    override val name: String,
    val validator: Validator
) : ClassModel {
    data class Validator(override val value: String) : Value<String>{
        val expression get() = value
            .split("/")
            .drop(1)
            .dropLast(1)
            .joinToString("/")
    }
}

data class EnumClass(
    override val name: String,
    val entries: Set<String>
) : ClassModel

data class UnionClass(
    override val name: String,
    val entries: List<String>,
) : ClassModel

data class EndpointClass(
    override val name: String,
    val path: String,
    val method: String,
    val functionName: String,
    val requestClasses: List<RequestClass>,
    val responseInterfaces: List<ResponseInterface>,
    val responseClasses: List<ResponseClass>,
    val requestMapper: RequestMapper,
    val responseMapper: ResponseMapper,
    val supers: List<ClassReference>
) : ClassModel {
    data class RequestClass(
        val name: String,
        val fields: List<FieldClass>,
        val requestAllArgsConstructor: RequestAllArgsConstructor,
        val requestParameterConstructor: RequestParameterConstructor,
        val supers: List<ClassReference>,
    ) {
        data class RequestAllArgsConstructor(
            val name: String,
            val parameters: List<Parameter>,
            val content: Content? = null
        )

        data class RequestParameterConstructor(
            val name: String,
            val parameters: List<Parameter>,
            val path: Path,
            val method: String,
            val query: List<String>,
            val headers: List<String>,
            val content: Content? = null
        )
    }

    data class RequestMapper(
        val name: String,
        val conditions: List<RequestCondition>
    ) {
        data class RequestCondition(
            val content: Content?,
            val responseReference: ClassReference,
            val isIterable: Boolean,
        )
    }

    data class ResponseClass(
        val name: String,
        val fields: List<FieldClass>,
        val responseAllArgsConstructor: ResponseAllArgsConstructor,
        val responseParameterConstructor: ResponseParameterConstructor,
        val `super`: ClassReference,
        val statusCode: String,
        val content: Content? = null
    ) {
        data class ResponseAllArgsConstructor(
            val name: String,
            val parameters: List<Parameter>,
            val statusCode: String,
            val content: Content? = null
        )

        data class ResponseParameterConstructor(
            val name: String,
            val parameters: List<Parameter>,
            val statusCode: String,
            val headers: List<String>,
            val content: Content? = null
        )
    }

    data class ResponseInterface(
        val name: ClassReference,
        val `super`: ClassReference,
    )

    data class ResponseMapper(
        val name: String,
        val conditions: List<ResponseCondition>
    ) {
        data class ResponseCondition(
            val statusCode: String,
            val content: Content?,
            val responseReference: ClassReference,
            val isIterable: Boolean,
        )
    }

    data class Content(
        val type: String,
        val reference: ClassReference
    )

    data class Path(override val value: List<Segment>) : Value<List<Path.Segment>> {
        sealed interface Segment
        data class Literal(override val value: String) : Value<String>, Segment
        data class Parameter(override val value: String) : Value<String>, Segment
    }
}

data class FieldClass(
    val identifier: String,
    val reference: ClassReference,
    val isOverride: Boolean = false,
    val isPrivate: Boolean = false,
    val isFinal: Boolean = false,
)

data class Parameter(
    val identifier: String,
    val reference: ClassReference,
)

sealed interface ClassReference {
    val isNullable: Boolean
    val isIterable: Boolean
    val isDictionary: Boolean
    val isOptional: Boolean
    val isInternal: Boolean

    data class Language(
        val primitive: Primitive,
        override val isNullable: Boolean = false,
        override val isIterable: Boolean = false,
        override val isDictionary: Boolean = false,
        override val isOptional: Boolean = false,
        override val isInternal: Boolean = false,
        val generics: Generics = Generics()
    ) : ClassReference {
        enum class Primitive { Any, Unit, String, Integer, Long, Double, Number, Boolean, Map, List }
    }

    data class Custom(
        val name: String,
        override val isNullable: Boolean = false,
        override val isIterable: Boolean = false,
        override val isDictionary: Boolean = false,
        override val isOptional: Boolean = false,
        override val isInternal: Boolean = false,
        val generics: Generics = Generics()
    ) : ClassReference

    data class Wirespec(
        val name: String,
        override val isNullable: Boolean = false,
        override val isIterable: Boolean = false,
        override val isDictionary: Boolean = false,
        override val isOptional: Boolean = false,
        override val isInternal: Boolean = false,
        val generics: Generics = Generics()
    ) : ClassReference

    data class Generics(
        val references: List<ClassReference> = emptyList()
    )
}
