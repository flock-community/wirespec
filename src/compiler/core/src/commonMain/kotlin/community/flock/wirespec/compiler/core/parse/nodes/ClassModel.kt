package community.flock.wirespec.compiler.core.parse.nodes

sealed interface ClassModel : Node

data class EndpointClass(
    val name: String,
    val path: String,
    val method: String,
    val requestClasses: List<RequestClass>,
    val requestMapper: RequestMapper,
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
    ){
        data class Condition(
            val contentType: String,
            val contentReference: Reference,
            val responseReference: Reference,
            val isIterable:Boolean,
        )
    }


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
    val nullable: Boolean
    val generics: Generics

    data class Language(
        val primitive: Primitive,
        override val nullable: Boolean = false,
        override val generics: Generics = Generics()
    ) : Reference {
        enum class Primitive { Any, Unit, String, Integer, Number, Boolean, Map, List }
    }

    data class Custom(
        val name: String,
        override val nullable: Boolean = false,
        override val generics: Generics = Generics()
    ) : Reference

    data class Generics(
        val references: List<Reference> = emptyList()
    )
}