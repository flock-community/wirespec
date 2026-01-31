package community.flock.wirespec.language.core

enum class Precision {
    P32,
    P64,
}

sealed interface Type {
    data class Integer(val precision: Precision = Precision.P32) : Type
    data class Number(val precision: Precision = Precision.P64) : Type
    object String : Type
    object Boolean : Type
    object Bytes : Type
    object Unit : Type
    data class Array(val elementType: Type) : Type
    data class Dict(val keyType: Type, val valueType: Type) : Type
    data class Custom(val name: kotlin.String, val generics: List<Type> = emptyList()) : Type
    data class Nullable(val type: Type) : Type
}

data class File(
    val name: String,
    val elements: List<Element>,
) : Element

sealed interface Element

data class Package(
    val path: String,
) : Element

data class Import(
    val path: String,
) : Element

data class Struct(
    val name: String,
    val fields: List<Field>,
    val constructors: List<Constructor> = emptyList(),
    val interfaces: List<Type.Custom> = emptyList(),
    val elements: List<Element> = emptyList(),
) : Element

data class Constructor(
    val parameters: List<Parameter>,
    val body: List<Statement>,
)

data class Field(
    val name: String,
    val type: Type,
)

data class Function(
    val name: String,
    val parameters: List<Parameter>,
    val returnType: Type?,
    val body: List<Statement>,
    val isAsync: Boolean = false,
    val isStatic: Boolean = false,
    val isOverride: Boolean = false,
) : Element

data class Static(
    val name: String,
    val elements: List<Element>,
    val extends: Type.Custom? = null,
) : Element

data class Interface(
    val name: String,
    val elements: List<Element>,
    val extends: Type.Custom? = null,
) : Element

data class Union(
    val name: String,
    val extends: Type.Custom? = null,
    val members: List<String> = emptyList(),
) : Element

data class Enum(
    val name: String,
    val extends: Type.Custom? = null,
    val entries: List<Entry>,
    val fields: List<Field> = emptyList(),
    val constructors: List<Constructor> = emptyList(),
    val elements: List<Element> = emptyList(),
) : Element {
    data class Entry(val name: String, val values: List<String>)
}

data class Parameter(
    val name: String,
    val type: Type,
)

sealed interface Statement : Expression
sealed interface Expression

data class RawExpression(val code: String) : Expression

data class PrintStatement(val expression: Expression) : Statement
data class ReturnStatement(val expression: Expression) : Statement
data class ConstructorStatement(val type: Type, val namedArguments: Map<String, Expression> = emptyMap()) :
    Statement,
    Expression
data class Call(val name: String, val arguments: Map<String, Expression> = emptyMap()) :
    Statement,
    Expression
data class Literal(val value: Any, val type: Type) :
    Statement,
    Expression
data class LiteralList(val values: List<Expression>, val type: Type) :
    Statement,
    Expression
data class LiteralMap(val values: Map<String, Expression>, val keyType: Type, val valueType: Type) :
    Statement,
    Expression
data class Assignment(val name: String, val value: Expression, val isProperty: Boolean = false) : Statement
data class ErrorStatement(val message: Expression) : Statement

data class Switch(
    val expression: Expression,
    val cases: List<Case>,
    val default: List<Statement>? = null,
) : Statement

data class Case(
    val value: Expression,
    val body: List<Statement>,
    val type: Type? = null,
    val variable: String? = null,
)
