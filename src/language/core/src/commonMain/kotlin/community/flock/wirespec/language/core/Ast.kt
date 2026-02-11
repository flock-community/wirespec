package community.flock.wirespec.language.core

enum class Precision {
    P32,
    P64,
}

sealed interface Type {
    data class Integer(val precision: Precision = Precision.P32) : Type
    data class Number(val precision: Precision = Precision.P64) : Type
    object Any : Type
    object String : Type
    object Boolean : Type
    object Bytes : Type
    object Unit : Type
    object Wildcard : Type
    data class Array(val elementType: Type) : Type
    data class Dict(val keyType: Type, val valueType: Type) : Type
    data class Custom(val name: kotlin.String, val generics: List<Type> = emptyList()) : Type
    data class Nullable(val type: Type) : Type
}

sealed interface Element

sealed interface HasName : Element {
    val name: String
}

interface HasElements {
    val elements: List<Element>
}

data class File(
    override val name: String,
    override val elements: List<Element>,
) : HasName,
    HasElements

data class Package(
    val path: String,
) : Element

data class Import(
    val path: String,
    val type: Type.Custom,
) : Element

data class Struct(
    override val name: String,
    val fields: List<Field>,
    val constructors: List<Constructor> = emptyList(),
    val interfaces: List<Type.Custom> = emptyList(),
    override val elements: List<Element> = emptyList(),
) : HasName,
    HasElements

data class Constructor(
    val parameters: List<Parameter>,
    val body: List<Statement>,
)

data class Field(
    val name: String,
    val type: Type,
    val isOverride: Boolean = false,
)

data class Function(
    override val name: String,
    val typeParameters: List<TypeParameter> = emptyList(),
    val parameters: List<Parameter>,
    val returnType: Type?,
    val body: List<Statement>,
    val isAsync: Boolean = false,
    val isStatic: Boolean = false,
    val isOverride: Boolean = false,
) : HasName

data class Static(
    override val name: String,
    override val elements: List<Element>,
    val extends: Type.Custom? = null,
) : HasName,
    HasElements

data class Interface(
    override val name: String,
    override val elements: List<Element>,
    val extends: List<Type.Custom> = emptyList(),
    val isSealed: Boolean = false,
    val typeParameters: List<TypeParameter> = emptyList(),
    val fields: List<Field> = emptyList(),
) : HasName,
    HasElements

data class Union(
    override val name: String,
    val extends: Type.Custom? = null,
    val members: List<Type.Custom> = emptyList(),
    val typeParameters: List<TypeParameter> = emptyList(),
) : HasName

data class Enum(
    override val name: String,
    val extends: Type.Custom? = null,
    val entries: List<Entry>,
    val fields: List<Field> = emptyList(),
    val constructors: List<Constructor> = emptyList(),
    override val elements: List<Element> = emptyList(),
) : HasName,
    HasElements {
    data class Entry(val name: String, val values: List<String>)
}

data class Parameter(
    val name: String,
    val type: Type,
)

data class TypeParameter(
    val type: Type,
    val extends: List<Type> = emptyList(),
)

sealed interface Statement : Expression
sealed interface Expression

data class RawExpression(val code: String) : Expression

// Raw element - allows injecting raw code as an Element
data class RawElement(val code: String) : Element

// Null literal - represents the null value
data object NullLiteral : Statement, Expression

// Nullable empty literal - represents the empty optional value (e.g., Optional.empty() in Java, null in Kotlin)
data object NullableEmpty : Statement, Expression

// Variable/identifier reference - represents a reference to a variable
data class VariableReference(val name: String) :
    Statement,
    Expression

// Field access - represents accessing a field, optionally on a receiver (e.g., request.body or just body)
data class FieldCall(
    val receiver: Expression? = null,
    val field: String,
) : Statement,
    Expression

// Function/method call - represents calling a function or method, optionally on a receiver
// If receiver is null, it's a standalone or static function call (e.g., fromRequest(...), java.util.Collections.emptyList())
// If receiver is present, it's a method call on an object (e.g., list.get(index))
data class FunctionCall(
    val receiver: Expression? = null,
    val typeArguments: List<Type> = emptyList(),
    val name: String,
    val arguments: Map<String, Expression> = emptyMap(),
) : Statement,
    Expression

// Array/map index access - represents bracket syntax (e.g., receiver[index])
data class ArrayIndexCall(
    val receiver: Expression,
    val index: Expression,
) : Statement,
    Expression

// Enum constant reference - represents an enum constant (e.g., Wirespec.Method.GET)
data class EnumReference(
    val enumType: Type.Custom,
    val entry: String,
) : Statement,
    Expression

// Enum value name access - gets the string name of an enum value
// In Java: .name(), in Kotlin: .name, in TypeScript: no-op (enums are already strings)
data class EnumValueCall(
    val expression: Expression,
) : Statement,
    Expression

// Binary operations - represents binary operators (e.g., "message" + status)
data class BinaryOp(
    val left: Expression,
    val operator: Operator,
    val right: Expression,
) : Statement,
    Expression {
    enum class Operator { PLUS, EQUALS, NOT_EQUALS }
}

// Type descriptor - represents a runtime type descriptor for serialization
// In Java this emits Wirespec.getType(Type.class, Container.class)
// In other languages it may emit different type descriptor patterns
data class TypeDescriptor(val type: Type) :
    Statement,
    Expression

data class PrintStatement(val expression: Expression) : Statement
data class ReturnStatement(val expression: Expression) : Statement
data class ConstructorStatement(val type: Type, val namedArguments: Map<String, Expression> = emptyMap()) :
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

data class NullCheck(
    val expression: Expression,
    val body: Expression,
    val alternative: Expression?,
) : Statement,
    Expression

data class NullableMap(
    val expression: Expression,
    val body: Expression,
    val alternative: Expression,
) : Statement,
    Expression

data class NullableOf(
    val expression: Expression,
) : Statement,
    Expression

data class Switch(
    val expression: Expression,
    val cases: List<Case>,
    val default: List<Statement>? = null,
    val variable: String? = null,
) : Statement

data class Case(
    val value: Expression,
    val body: List<Statement>,
    val type: Type? = null,
)
