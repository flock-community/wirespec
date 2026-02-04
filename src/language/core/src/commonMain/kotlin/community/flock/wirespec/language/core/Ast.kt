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
    object Wildcard : Type
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
    val typeParameters: List<Type> = emptyList(),
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
    val isSealed: Boolean = false,
    val typeParameters: List<String> = emptyList(),
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

// Raw element - allows injecting raw code as an Element
data class RawElement(val code: String) : Element

// Null literal - represents the null value
data object NullLiteral : Statement, Expression

// Variable/identifier reference - represents a reference to a variable
data class VariableReference(val name: String) :
    Statement,
    Expression

// Property access - represents accessing a property on a receiver (e.g., request.body())
data class PropertyAccess(
    val receiver: Expression,
    val property: String,
) : Statement,
    Expression

// Method call on receiver - represents calling a method on an object (e.g., list.get(index))
// If receiver is null, it's a simple function call (e.g., fromRequest(...))
data class MethodCall(
    val receiver: Expression? = null,
    val typeArguments: List<Type> = emptyList(),
    val method: String,
    val arguments: Map<String, Expression> = emptyMap(),
) : Statement,
    Expression

// Enum constant reference - represents an enum constant (e.g., Wirespec.Method.GET)
data class EnumReference(
    val enumType: Type.Custom,
    val entry: String,
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

// Static/qualified method calls - represents static method calls (e.g., java.util.Collections.emptyMap())
data class StaticCall(
    val qualifiedName: String,
    val typeArguments: List<Type> = emptyList(),
    val arguments: Map<String, Expression> = emptyMap(),
) : Statement,
    Expression

// Class literal - represents a class reference (e.g., String.class)
data class ClassLiteral(val type: Type) :
    Statement,
    Expression

// Type descriptor - represents a runtime type descriptor for serialization
// In Java this emits Wirespec.getType(Type.class, Container.class)
// In other languages it may emit different type descriptor patterns
data class TypeDescriptor(val type: Type) :
    Statement,
    Expression

// Anonymous class - represents an anonymous class instantiation with method implementations
data class AnonymousClass(
    val baseType: Type.Custom,
    val typeArguments: List<Type> = emptyList(),
    val methods: List<Function>,
) : Statement,
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
