package community.flock.wirespec.ir.converter

import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.ir.core.ClassReference
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.Expression
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.IfExpression
import community.flock.wirespec.ir.core.Literal
import community.flock.wirespec.ir.core.LiteralList
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.NullLiteral
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.file
import community.flock.wirespec.compiler.core.parse.ast.Enum as EnumWirespec
import community.flock.wirespec.compiler.core.parse.ast.Reference as ReferenceWirespec
import community.flock.wirespec.compiler.core.parse.ast.Refined as RefinedWirespec
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeWirespec
import community.flock.wirespec.compiler.core.parse.ast.Union as UnionWirespec

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

internal fun pathPlus(segment: String): BinaryOp = BinaryOp(
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
                } else {
                    NullLiteral
                },
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

fun TypeWirespec.convertToGenerator(): File {
    val generatorName = identifier.toGeneratorName()
    val typeName = identifier.value

    return file(generatorName) {
        namespace(generatorName) {
            function("generate") {
                arg("path", list(string))
                arg("generator", type("Wirespec.Generator"))
                returnType(type(typeName))
                returns(
                    ConstructorStatement(
                        type = Type.Custom(typeName),
                        namedArguments = shape.value.associate { field ->
                            val fieldName = field.identifier.toFieldName()
                            fieldName to field.reference.toGeneratorExpression(typeName, field.identifier.value)
                        },
                    ),
                )
            }
        }
    }
}

private fun ReferenceWirespec.toGeneratorExpression(typeName: String, fieldNameStr: String): Expression {
    val nullableCheck: Expression = generatorCallExpression(
        typeName,
        fieldNameStr,
        ConstructorStatement(
            type = Type.Custom("Wirespec.GeneratorFieldNullable"),
            namedArguments = mapOf(
                Name.of("inner") to when (val ref = this) {
                    is ReferenceWirespec.Primitive -> ref.toFieldDescriptor()
                    is ReferenceWirespec.Iterable -> ConstructorStatement(
                        type = Type.Custom("Wirespec.GeneratorFieldArray"),
                        namedArguments = mapOf(Name.of("inner") to ref.reference.toFieldDescriptorOrNull()),
                    )
                    is ReferenceWirespec.Dict -> ConstructorStatement(
                        type = Type.Custom("Wirespec.GeneratorFieldDict"),
                        namedArguments = mapOf(
                            Name.of("key") to NullLiteral,
                            Name.of("value") to ref.reference.toFieldDescriptorOrNull(),
                        ),
                    )
                    is ReferenceWirespec.Custom, is ReferenceWirespec.Any, is ReferenceWirespec.Unit -> NullLiteral
                },
            ),
        ),
    )

    val nonNullExpr: Expression = when (val ref = this) {
        is ReferenceWirespec.Primitive -> generatorCallExpression(typeName, fieldNameStr, ref.toFieldDescriptor())
        is ReferenceWirespec.Custom -> FunctionCall(
            receiver = RawExpression("${ref.value}Generator"),
            name = Name.of("generate"),
            arguments = mapOf(
                Name.of("path") to pathPlus(fieldNameStr),
                Name.of("generator") to VariableReference(Name.of("generator")),
            ),
        )
        is ReferenceWirespec.Iterable -> generatorCallExpression(
            typeName,
            fieldNameStr,
            ConstructorStatement(
                type = Type.Custom("Wirespec.GeneratorFieldArray"),
                namedArguments = mapOf(Name.of("inner") to ref.reference.toFieldDescriptorOrNull()),
            ),
        )
        is ReferenceWirespec.Dict -> generatorCallExpression(
            typeName,
            fieldNameStr,
            ConstructorStatement(
                type = Type.Custom("Wirespec.GeneratorFieldDict"),
                namedArguments = mapOf(
                    Name.of("key") to NullLiteral,
                    Name.of("value") to ref.reference.toFieldDescriptorOrNull(),
                ),
            ),
        )
        is ReferenceWirespec.Any, is ReferenceWirespec.Unit -> NullLiteral
    }

    return if (this.isNullable) {
        IfExpression(
            condition = nullableCheck,
            thenExpr = NullLiteral,
            elseExpr = nonNullExpr,
        )
    } else {
        nonNullExpr
    }
}

fun RefinedWirespec.convertToGenerator(): File {
    val generatorName = identifier.toGeneratorName()
    val typeName = identifier.value

    return file(generatorName) {
        namespace(generatorName) {
            function("generate") {
                arg("path", list(string))
                arg("generator", type("Wirespec.Generator"))
                returnType(type(typeName))
                returns(
                    ConstructorStatement(
                        type = Type.Custom(typeName),
                        namedArguments = mapOf(
                            Name.of("value") to FunctionCall(
                                receiver = VariableReference(Name.of("generator")),
                                name = Name.of("generate"),
                                arguments = mapOf(
                                    Name.of("path") to pathPlus("value"),
                                    Name.of("type") to ClassReference(Type.Custom(typeName)),
                                    Name.of("field") to reference.toFieldDescriptor(),
                                ),
                            ),
                        ),
                    ),
                )
            }
        }
    }
}

fun EnumWirespec.convertToGenerator(): File {
    val generatorName = identifier.toGeneratorName()
    val typeName = identifier.value

    return file(generatorName) {
        namespace(generatorName) {
            function("generate") {
                arg("path", list(string))
                arg("generator", type("Wirespec.Generator"))
                returnType(type(typeName))
                returns(
                    ConstructorStatement(
                        type = Type.Custom(typeName),
                        namedArguments = mapOf(
                            Name.of("label") to FunctionCall(
                                receiver = VariableReference(Name.of("generator")),
                                name = Name.of("generate"),
                                arguments = mapOf(
                                    Name.of("path") to pathPlus("value"),
                                    Name.of("type") to ClassReference(Type.Custom(typeName)),
                                    Name.of("field") to ConstructorStatement(
                                        type = Type.Custom("Wirespec.GeneratorFieldEnum"),
                                        namedArguments = mapOf(
                                            Name.of("values") to LiteralList(
                                                values = entries.map { Literal(it, Type.String) },
                                                type = Type.String,
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                )
            }
        }
    }
}

fun UnionWirespec.convertToGenerator(): File {
    val generatorName = identifier.toGeneratorName()
    val typeName = identifier.value
    val variantNames = entries.filterIsInstance<ReferenceWirespec.Custom>().map { it.value }

    return file(generatorName) {
        namespace(generatorName) {
            function("generate") {
                arg("path", list(string))
                arg("generator", type("Wirespec.Generator"))
                returnType(type(typeName))
                assign(
                    "variant",
                    FunctionCall(
                        receiver = VariableReference(Name.of("generator")),
                        name = Name.of("generate"),
                        arguments = mapOf(
                            Name.of("path") to pathPlus("variant"),
                            Name.of("type") to ClassReference(Type.Custom(typeName)),
                            Name.of("field") to ConstructorStatement(
                                type = Type.Custom("Wirespec.GeneratorFieldUnion"),
                                namedArguments = mapOf(
                                    Name.of("variants") to LiteralList(
                                        values = variantNames.map { Literal(it, Type.String) },
                                        type = Type.String,
                                    ),
                                ),
                            ),
                        ),
                    ),
                )
                switch(VariableReference(Name.of("variant"))) {
                    for (variantName in variantNames) {
                        case(Literal(variantName, Type.String)) {
                            returns(
                                FunctionCall(
                                    receiver = RawExpression("${variantName}Generator"),
                                    name = Name.of("generate"),
                                    arguments = mapOf(
                                        Name.of("path") to BinaryOp(
                                            VariableReference(Name.of("path")),
                                            BinaryOp.Operator.PLUS,
                                            Literal(variantName, Type.String),
                                        ),
                                        Name.of("generator") to VariableReference(Name.of("generator")),
                                    ),
                                ),
                            )
                        }
                    }
                }
                error(Literal("Unknown variant", Type.String))
            }
        }
    }
}
