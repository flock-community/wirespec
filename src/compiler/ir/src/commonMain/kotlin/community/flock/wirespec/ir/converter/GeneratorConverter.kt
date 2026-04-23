package community.flock.wirespec.ir.converter

import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.ir.core.ClassReference
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.Expression
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Literal
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.NullLiteral
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.compiler.core.parse.ast.Reference as ReferenceWirespec

internal fun Identifier.toGeneratorName(): Name = when (this) {
    is FieldIdentifier -> {
        val parts = value.split(Regex("[.\\s-]+")).filter { it.isNotEmpty() }
        Name(parts + "Generator")
    }
    is DefinitionIdentifier -> Name(
        Name.of(value).parts.filter { part -> part.any { it.isLetterOrDigit() } } + "Generator",
    )
}

internal fun FieldIdentifier.toFieldName(): Name {
    val parts = value.split(Regex("[.\\s-]+")).filter { it.isNotEmpty() }
    return Name(parts)
}

internal fun pathPlus(segment: String): BinaryOp =
    BinaryOp(
        VariableReference(Name.of("path")),
        BinaryOp.Operator.PLUS,
        Literal(segment, Type.String),
    )

internal fun ReferenceWirespec.Primitive.toFieldDescriptor(): Expression = when (val t = type) {
    is ReferenceWirespec.Primitive.Type.String -> {
        val constraint = t.constraint
        ConstructorStatement(
            type = Type.Custom("Wirespec.GeneratorFieldString"),
            namedArguments = mapOf(
                Name.of("regex") to if (constraint != null) {
                    Literal(
                        constraint.value.split("/").drop(1).dropLast(1).joinToString("/"),
                        Type.String,
                    )
                } else NullLiteral,
            ),
        )
    }
    is ReferenceWirespec.Primitive.Type.Integer -> ConstructorStatement(
        type = Type.Custom("Wirespec.GeneratorFieldInteger"),
        namedArguments = mapOf(
            Name.of("min") to (t.constraint?.min?.let { Literal(it.toLong(), Type.Integer()) } ?: NullLiteral),
            Name.of("max") to (t.constraint?.max?.let { Literal(it.toLong(), Type.Integer()) } ?: NullLiteral),
        ),
    )
    is ReferenceWirespec.Primitive.Type.Number -> ConstructorStatement(
        type = Type.Custom("Wirespec.GeneratorFieldNumber"),
        namedArguments = mapOf(
            Name.of("min") to (t.constraint?.min?.let { Literal(it.toDouble(), Type.Number()) } ?: NullLiteral),
            Name.of("max") to (t.constraint?.max?.let { Literal(it.toDouble(), Type.Number()) } ?: NullLiteral),
        ),
    )
    ReferenceWirespec.Primitive.Type.Boolean -> ConstructorStatement(
        type = Type.Custom("Wirespec.GeneratorFieldBoolean"),
    )
    ReferenceWirespec.Primitive.Type.Bytes -> ConstructorStatement(
        type = Type.Custom("Wirespec.GeneratorFieldBytes"),
    )
}

internal fun ReferenceWirespec.toFieldDescriptorOrNull(): Expression = when (this) {
    is ReferenceWirespec.Primitive -> toFieldDescriptor()
    else -> NullLiteral
}

internal fun generatorCallExpression(
    typeName: String,
    fieldNameStr: String,
    fieldDescriptor: Expression,
): FunctionCall = FunctionCall(
    receiver = VariableReference(Name.of("generator")),
    name = Name.of("generate"),
    arguments = mapOf(
        Name.of("path") to pathPlus(fieldNameStr),
        Name.of("type") to ClassReference(Type.Custom(typeName)),
        Name.of("field") to fieldDescriptor,
    ),
)
