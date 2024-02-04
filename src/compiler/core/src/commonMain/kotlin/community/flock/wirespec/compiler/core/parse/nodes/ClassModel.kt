package community.flock.wirespec.compiler.core.parse.nodes

sealed interface ClassModel : Node

data class EndpointClass(
    val name: String,
    val path: String,
    val method: String,
    val requestClasses: List<RequestClass>,
    val responseInterfaces: List<ResponseInterface>,
    val responseClasses: List<ResponseClass>,
    val requestMapper: RequestMapper,
    val responseMapper: ResponseMapper,
    val supers: List<Reference>,
) : ClassModel {
    data class RequestClass(
        val name: String,
        val fields: List<Field>,
        val primaryConstructor: PrimaryConstructor,
        val secondaryConstructor: SecondaryConstructor,
        val supers: List<Reference>,
    ){
        data class PrimaryConstructor(
            val name: String,
            val parameters: List<Parameter>,
            val content: Content? = null
        )
        data class SecondaryConstructor(
            val name: String,
            val parameters: List<Parameter>,
            val path: Path,
            val method: String,
            val query: String,
            val headers: String,
            val content: Content? = null
        )
    }

    data class RequestMapper(
        val name: String,
        val conditions: List<RequestCondition>
    ) {
        data class RequestCondition(
            val content: Content?,
            val responseReference: Reference,
            val isIterable: Boolean,
        )
    }

    data class ResponseClass(
        val name: String,
        val fields: List<Field>,
        val allArgsConstructor: AllArgsConstructor,
        val returnReference: Reference,
        val statusCode: String,
        val content: Content? = null
    ){
        data class AllArgsConstructor(
            val name: String,
            val parameters: List<Parameter>,
            val statusCode: String,
            val content: Content? = null
        )
    }

    data class ResponseInterface(
        val name: Reference,
        val `super`: Reference,
    )

    data class ResponseMapper(
        val name: String,
        val conditions: List<ResponseCondition>
    ) {
        data class ResponseCondition(
            val statusCode: String,
            val content: Content?,
            val responseReference: Reference,
            val isIterable: Boolean,
        )
    }
    data class Content(
        val type: String,
        val reference: Reference
    )

    data class Path(val value: List<Segment>){
        sealed interface Segment
        data class Literal(val value: String): Segment
        data class Parameter(val value: String): Segment
    }
}

data class Field(
    val identifier: String,
    val reference: Reference,
    val override: Boolean = false,
)

data class Parameter(
    val identifier: String,
    val reference: Reference,
)

sealed interface Reference {

    data class Language(
        val primitive: Primitive,
        val nullable: Boolean = false,
        val generics: Generics = Generics()
    ) : Reference {
        enum class Primitive { Any, Unit, String, Integer, Long, Number, Boolean, Map, List }
    }

    data class Custom(
        val name: String,
        val nullable: Boolean = false,
        val generics: Generics = Generics()
    ) : Reference

    data class Generics(
        val references: List<Reference> = emptyList()
    )
}