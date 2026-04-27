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
import community.flock.wirespec.ir.core.ListConcat
import community.flock.wirespec.ir.core.Literal
import community.flock.wirespec.ir.core.LiteralList
import community.flock.wirespec.ir.core.MapExpression
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.NullLiteral
import community.flock.wirespec.ir.core.NullableEmpty
import community.flock.wirespec.ir.core.NullableOf
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.StringTemplate
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

// List-append: use ListConcat so Java emits `Stream.concat(...)` instead of a
// bogus `path + "segment"` (which Kotlin accepts but Java evaluates as
// List.toString() + String).
internal fun pathPlus(segment: String): Expression = ListConcat(
    listOf(
        VariableReference(Name.of("path")),
        LiteralList(listOf(Literal(segment, Type.String)), Type.String),
    ),
)

internal fun ReferenceWirespec.Primitive.toFieldDescriptor(): Expression = when (val t = type) {
    is ReferenceWirespec.Primitive.Type.String -> {
        val constraint = t.constraint
        ConstructorStatement(
            type = Type.Custom("Wirespec.GeneratorFieldString"),
            namedArguments = mapOf(
                // Optional<String> in Java, String? in Kotlin — NullableEmpty/NullableOf
                // are the portable wrappers.
                Name.of("regex") to if (constraint != null) {
                    NullableOf(
                        Literal(
                            constraint.value.split("/").drop(1).dropLast(1).joinToString("/"),
                            Type.String,
                        ),
                    )
                } else {
                    NullableEmpty
                },
            ),
        )
    }
    is ReferenceWirespec.Primitive.Type.Integer -> ConstructorStatement(
        type = Type.Custom("Wirespec.GeneratorFieldInteger"),
        namedArguments = mapOf(
            Name.of("min") to (t.constraint?.min?.let { NullableOf(Literal(it.toLong(), Type.Integer())) } ?: NullableEmpty),
            Name.of("max") to (t.constraint?.max?.let { NullableOf(Literal(it.toLong(), Type.Integer())) } ?: NullableEmpty),
        ),
    )
    is ReferenceWirespec.Primitive.Type.Number -> ConstructorStatement(
        type = Type.Custom("Wirespec.GeneratorFieldNumber"),
        namedArguments = mapOf(
            Name.of("min") to (t.constraint?.min?.let { NullableOf(Literal(it.toDouble(), Type.Number())) } ?: NullableEmpty),
            Name.of("max") to (t.constraint?.max?.let { NullableOf(Literal(it.toDouble(), Type.Number())) } ?: NullableEmpty),
        ),
    )
    ReferenceWirespec.Primitive.Type.Boolean -> ConstructorStatement(
        type = Type.Custom("Wirespec.GeneratorFieldBoolean"),
    )
    ReferenceWirespec.Primitive.Type.Bytes -> ConstructorStatement(
        type = Type.Custom("Wirespec.GeneratorFieldBytes"),
    )
}

// Produces an Optional<GeneratorField<?>>-shaped value: NullableOf(desc) when
// primitive, NullableEmpty otherwise. In Kotlin this round-trips to a
// `GeneratorField<*>?`; in Java it becomes `Optional<GeneratorField<?>>`.
internal fun ReferenceWirespec.toFieldDescriptorOrNull(): Expression = when (this) {
    is ReferenceWirespec.Primitive -> NullableOf(toFieldDescriptor())
    else -> NullableEmpty
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
                // `inner` is Optional<GeneratorField<?>> in Java — wrap non-empty
                // values with NullableOf so Java emits Optional.of(...).
                Name.of("inner") to when (val ref = this) {
                    is ReferenceWirespec.Primitive -> NullableOf(ref.toFieldDescriptor())
                    is ReferenceWirespec.Iterable -> NullableOf(
                        ConstructorStatement(
                            type = Type.Custom("Wirespec.GeneratorFieldArray"),
                            namedArguments = mapOf(Name.of("inner") to ref.reference.toFieldDescriptorOrNull()),
                        ),
                    )
                    is ReferenceWirespec.Dict -> NullableOf(
                        ConstructorStatement(
                            type = Type.Custom("Wirespec.GeneratorFieldDict"),
                            namedArguments = mapOf(
                                Name.of("key") to NullableEmpty,
                                Name.of("value") to ref.reference.toFieldDescriptorOrNull(),
                            ),
                        ),
                    )
                    is ReferenceWirespec.Custom, is ReferenceWirespec.Any, is ReferenceWirespec.Unit -> NullableEmpty
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
        is ReferenceWirespec.Iterable -> {
            // Emit iteration: (0 until generator.generate(path + "field", Type::class, GeneratorFieldArray(...))).map { i -> <element-expr> }
            // The outer callback returns the element count; for each index i we call the
            // element generator and build `path + "field" + i.toString()`.
            val countCall = generatorCallExpression(
                typeName,
                fieldNameStr,
                ConstructorStatement(
                    type = Type.Custom("Wirespec.GeneratorFieldArray"),
                    namedArguments = mapOf(Name.of("inner") to ref.reference.toFieldDescriptorOrNull()),
                ),
            )
            // Stringify the index portably: `i.toString()` compiles in Kotlin
            // but not in Java (primitive int has no method invocation). A
            // StringTemplate with an empty text part + expr yields `"${i}"` in
            // Kotlin and `"" + i` in Java, both producing a String.
            val indexAsString = StringTemplate(
                listOf(
                    StringTemplate.Part.Text(""),
                    StringTemplate.Part.Expr(VariableReference(Name.of("i"))),
                ),
            )
            val indexedPath = ListConcat(
                listOf(
                    VariableReference(Name.of("path")),
                    LiteralList(
                        listOf(Literal(fieldNameStr, Type.String), indexAsString),
                        Type.String,
                    ),
                ),
            )
            val elementExpr: Expression = when (val inner = ref.reference) {
                is ReferenceWirespec.Custom -> FunctionCall(
                    receiver = RawExpression("${inner.value}Generator"),
                    name = Name.of("generate"),
                    arguments = mapOf(
                        Name.of("path") to indexedPath,
                        Name.of("generator") to VariableReference(Name.of("generator")),
                    ),
                )
                is ReferenceWirespec.Primitive -> FunctionCall(
                    receiver = VariableReference(Name.of("generator")),
                    name = Name.of("generate"),
                    arguments = mapOf(
                        Name.of("path") to indexedPath,
                        Name.of("type") to ClassReference(Type.Custom(typeName)),
                        Name.of("field") to inner.toFieldDescriptor(),
                    ),
                )
                else -> NullLiteral
            }
            MapExpression(
                receiver = BinaryOp(
                    // int 0, not Long: Java's IntStream.range takes ints.
                    Literal(0, Type.Integer()),
                    BinaryOp.Operator.UNTIL,
                    countCall,
                ),
                variable = Name.of("i"),
                body = elementExpr,
            )
        }
        is ReferenceWirespec.Dict -> generatorCallExpression(
            typeName,
            fieldNameStr,
            ConstructorStatement(
                type = Type.Custom("Wirespec.GeneratorFieldDict"),
                namedArguments = mapOf(
                    Name.of("key") to NullableEmpty,
                    Name.of("value") to ref.reference.toFieldDescriptorOrNull(),
                ),
            ),
        )
        is ReferenceWirespec.Any, is ReferenceWirespec.Unit -> NullLiteral
    }

    return if (this.isNullable) {
        // Nullable model fields are `T?` in Kotlin but `Optional<T>` in Java.
        // Wrap both branches so each language emits the correct container type.
        IfExpression(
            condition = nullableCheck,
            thenExpr = NullableEmpty,
            elseExpr = NullableOf(nonNullExpr),
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
                    FunctionCall(
                        receiver = RawExpression(typeName),
                        name = Name.of("valueOf"),
                        arguments = mapOf(
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
                                        Name.of("path") to pathPlus(variantName),
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
