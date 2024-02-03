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
        val constructors: List<Constructor>,
        val supers: List<Reference>,
    )

    data class RequestMapper(
        val name: String,
        val conditions: List<Condition>
    ) {
        data class Condition(
            val content: Content?,
            val responseReference: Reference,
            val isIterable: Boolean,
        )
    }

    data class ResponseClass(
        val name: String,
        val fields: List<Field>,
        val returnReference: Reference,
        val statusCode: String,
        val content: Content? = null
    )

    data class ResponseInterface(
        val name: Reference,
        val `super`: Reference,
    )

    data class ResponseMapper(
        val name: String,
        val conditions: List<Condition>
    ) {
        data class Condition(
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
}

data class Constructor(
    val name: String,
    val fields: List<Parameter>,
    val body: List<Statement>
)

data class Field(
    val identifier: String,
    val reference: Reference,
    val override: Boolean = false,
)

data class Parameter(
    val identifier: String,
    val reference: Reference,
)

sealed interface Statement {
    data class AssignField(
        val value: String,
        val statement: Statement
    ) : Statement

    data class Initialize(
        val reference: Reference,
        val parameters: List<String>
    ) : Statement

    data class Literal(
        val value: String,
    ) : Statement

    data class Variable(
        val value: String,
    ) : Statement

    data class Concat(
        val values: List<Statement>,
    ) : Statement
}

sealed interface Reference {

    data class Language(
        val primitive: Primitive,
        val nullable: Boolean = false,
        val generics: Generics = Generics()
    ) : Reference {
        enum class Primitive { Any, Unit, String, Integer, Number, Boolean, Map, List }
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