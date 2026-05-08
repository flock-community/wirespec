package community.flock.wirespec.ir.converter

import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.ir.core.ClassReference
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.Expression
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Lambda
import community.flock.wirespec.ir.core.ListConcat
import community.flock.wirespec.ir.core.Literal
import community.flock.wirespec.ir.core.LiteralList
import community.flock.wirespec.ir.core.LiteralMap
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.NullLiteral
import community.flock.wirespec.ir.core.NullableEmpty
import community.flock.wirespec.ir.core.NullableOf
import community.flock.wirespec.ir.core.Parameter
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.file
import community.flock.wirespec.compiler.core.parse.ast.Annotation as AnnotationWirespec
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

internal fun ReferenceWirespec.Primitive.toFieldDescriptor(annotations: List<AnnotationWirespec>): Expression {
    val annotationsArg = annotationsToIrList(annotations)
    return when (val t = type) {
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
                    Name.of("annotations") to annotationsArg,
                ),
            )
        }
        is ReferenceWirespec.Primitive.Type.Integer -> ConstructorStatement(
            type = Type.Custom("Wirespec.GeneratorFieldInteger"),
            namedArguments = mapOf(
                Name.of("min") to (t.constraint?.min?.let { NullableOf(Literal(it.toLong(), Type.Integer())) } ?: NullableEmpty),
                Name.of("max") to (t.constraint?.max?.let { NullableOf(Literal(it.toLong(), Type.Integer())) } ?: NullableEmpty),
                Name.of("annotations") to annotationsArg,
            ),
        )
        is ReferenceWirespec.Primitive.Type.Number -> ConstructorStatement(
            type = Type.Custom("Wirespec.GeneratorFieldNumber"),
            namedArguments = mapOf(
                Name.of("min") to (t.constraint?.min?.let { NullableOf(Literal(it.toDouble(), Type.Number())) } ?: NullableEmpty),
                Name.of("max") to (t.constraint?.max?.let { NullableOf(Literal(it.toDouble(), Type.Number())) } ?: NullableEmpty),
                Name.of("annotations") to annotationsArg,
            ),
        )
        ReferenceWirespec.Primitive.Type.Boolean -> ConstructorStatement(
            type = Type.Custom("Wirespec.GeneratorFieldBoolean"),
            namedArguments = mapOf(Name.of("annotations") to annotationsArg),
        )
        ReferenceWirespec.Primitive.Type.Bytes -> ConstructorStatement(
            type = Type.Custom("Wirespec.GeneratorFieldBytes"),
            namedArguments = mapOf(Name.of("annotations") to annotationsArg),
        )
    }
}

private val INTEGER_REGEX = Regex("^-?[0-9]+$")
private val DOUBLE_REGEX = Regex("^-?[0-9]+\\.[0-9]+([eE]-?[0-9]+)?$")

internal fun coerceAnnotationValueLiteral(raw: String): Literal = when {
    raw == "true" -> Literal(true, Type.Boolean)
    raw == "false" -> Literal(false, Type.Boolean)
    INTEGER_REGEX.matches(raw) -> Literal(raw.toLong(), Type.Integer())
    DOUBLE_REGEX.matches(raw) -> Literal(raw.toDouble(), Type.Number())
    else -> Literal(raw, Type.String)
}

private fun annotationValueToIrExpression(value: AnnotationWirespec.Value): Expression = when (value) {
    is AnnotationWirespec.Value.Single -> coerceAnnotationValueLiteral(value.value)
    is AnnotationWirespec.Value.Array -> LiteralList(
        values = value.value.map { coerceAnnotationValueLiteral(it.value) },
        type = Type.Any,
    )
    is AnnotationWirespec.Value.Dict -> LiteralMap(
        values = value.value.associate { it.name to annotationValueToIrExpression(it.value) },
        keyType = Type.String,
        valueType = Type.Any,
    )
}

internal fun AnnotationWirespec.toIrLiteralMap(): LiteralMap = LiteralMap(
    values = mapOf(
        "name" to Literal(name, Type.String),
        "parameters" to LiteralMap(
            values = parameters.associate { it.name to annotationValueToIrExpression(it.value) },
            keyType = Type.String,
            valueType = Type.Any,
        ),
    ),
    keyType = Type.String,
    valueType = Type.Any,
)

internal fun annotationsToIrList(annotations: List<AnnotationWirespec>): LiteralList = LiteralList(
    values = annotations.map { it.toIrLiteralMap() },
    type = Type.Dict(Type.String, Type.Any),
)

// Produces an Optional<GeneratorField<?>>-shaped value: NullableOf(desc) when
// primitive, NullableEmpty otherwise. In Kotlin this round-trips to a
// `GeneratorField<*>?`; in Java it becomes `Optional<GeneratorField<?>>`.
internal fun ReferenceWirespec.toFieldDescriptorOrNull(annotations: List<AnnotationWirespec>): Expression = when (this) {
    is ReferenceWirespec.Primitive -> NullableOf(toFieldDescriptor(annotations))
    else -> NullableEmpty
}

internal fun generatorCallExpression(
    fieldNameStr: String,
    fieldDescriptor: Expression,
): FunctionCall = FunctionCall(
    receiver = VariableReference(Name.of("generator")),
    name = Name.of("generate"),
    arguments = mapOf(
        Name.of("path") to pathPlus(fieldNameStr),
        Name.of("field") to fieldDescriptor,
    ),
)

// Build the per-field annotations map for a Custom reference's target.
// Returns the inner field-name → annotations map for record (Type) targets.
// For Refined/Enum targets the wrapped value is emitted under the synthetic
// `"value"` key, so [fieldAnnotations] (the annotations on the field that
// references the target — e.g. `@Seed id: ProjectId`) propagate down to the
// leaf primitive's GeneratorField via the SeededGenerator's pendingSeed
// mechanism. Returns empty for Union/Channel targets or when no module is
// supplied.
private fun targetFieldAnnotationsMap(
    targetName: String,
    fieldAnnotations: List<AnnotationWirespec>,
    module: Module?,
): Map<String, List<AnnotationWirespec>> {
    if (module == null) return emptyMap()
    val target = module.statements.firstOrNull { it.identifier.value == targetName } ?: return emptyMap()
    return when (target) {
        is TypeWirespec -> target.shape.value.associate { it.identifier.value to it.annotations }
        is RefinedWirespec, is EnumWirespec ->
            if (fieldAnnotations.isEmpty()) emptyMap() else mapOf("value" to fieldAnnotations)
        else -> emptyMap()
    }
}

fun TypeWirespec.convertToGenerator(module: Module? = null): File {
    val generatorName = identifier.toGeneratorName()
    val typeName = identifier.value

    return file(generatorName) {
        namespace(generatorName) {
            function("generate") {
                arg("generator", type("Wirespec.Generator"))
                arg("path", list(string))
                returnType(type(typeName))
                returns(
                    ConstructorStatement(
                        type = Type.Custom(typeName),
                        namedArguments = shape.value.associate { field ->
                            val fieldName = field.identifier.toFieldName()
                            fieldName to field.reference.toGeneratorExpression(
                                typeName,
                                field.identifier.value,
                                field.annotations,
                                module,
                            )
                        },
                    ),
                )
            }
        }
    }
}

// Each `generate` thunk takes a single path parameter. We number them by lambda nesting
// depth (`p0`, `p1`, ...) so nested lambdas don't shadow outer ones — Java disallows lambda
// parameters that shadow an enclosing method's `path` parameter or another lambda's parameter.
private fun lambdaPathName(depth: Int): Name = Name.of("p$depth")
private fun lambdaPathParam(depth: Int): List<Parameter> = listOf(Parameter(lambdaPathName(depth), Type.Array(Type.String)))
private fun lambdaPathRef(depth: Int): Expression = VariableReference(lambdaPathName(depth))

// `GeneratorFieldShape(annotations = { ... }, generate = { p -> XxxGenerator.generate(generator, p) }, type = <Target>::class)`.
// `targetFieldAnnotations` maps each field of the target record to its annotation list;
// the lambda forwards its path parameter to the downstream *Generator. `type` lets
// consumers (e.g. a kotest- or serialization-backed `Wirespec.Generator`) recover
// the runtime class of the shape being generated.
private fun shapeConstructor(
    downstreamGeneratorName: String,
    targetFieldAnnotations: Map<String, List<AnnotationWirespec>>,
    depth: Int,
): ConstructorStatement = ConstructorStatement(
    type = Type.Custom("Wirespec.GeneratorFieldShape"),
    namedArguments = mapOf(
        Name.of("annotations") to LiteralMap(
            values = targetFieldAnnotations.mapValues { (_, anns) -> annotationsToIrList(anns) },
            keyType = Type.String,
            valueType = Type.Array(Type.Dict(Type.String, Type.Any)),
        ),
        Name.of("generate") to Lambda(
            parameters = lambdaPathParam(depth),
            body = FunctionCall(
                receiver = RawExpression("${downstreamGeneratorName}Generator"),
                name = Name.of("generate"),
                arguments = mapOf(
                    Name.of("generator") to VariableReference(Name.of("generator")),
                    Name.of("path") to lambdaPathRef(depth),
                ),
            ),
        ),
        Name.of("type") to ClassReference(Type.Custom(downstreamGeneratorName)),
    ),
)

// Build a `generator.generate(<pathExpr>, ...)` for [ref]. Recursive sub-calls
// (Array element, Dict value, Nullable wrapped) live inside a path-parametrized
// lambda body, so they use the next-deeper lambda's path reference. Field
// annotations propagate through wrappers down to the leaf descriptor.
private fun buildLeafExpr(
    ref: ReferenceWirespec,
    pathExpr: Expression,
    typeName: String,
    annotations: List<AnnotationWirespec>,
    module: Module?,
    depth: Int,
): Expression = when (ref) {
    is ReferenceWirespec.Primitive -> FunctionCall(
        receiver = VariableReference(Name.of("generator")),
        name = Name.of("generate"),
        arguments = mapOf(
            Name.of("path") to pathExpr,
            Name.of("field") to ref.toFieldDescriptor(annotations),
        ),
    )
    is ReferenceWirespec.Custom -> FunctionCall(
        receiver = VariableReference(Name.of("generator")),
        name = Name.of("generate"),
        arguments = mapOf(
            Name.of("path") to pathExpr,
            Name.of("field") to shapeConstructor(
                ref.value,
                targetFieldAnnotationsMap(ref.value, annotations, module),
                depth + 1,
            ),
        ),
    )
    is ReferenceWirespec.Iterable -> FunctionCall(
        receiver = VariableReference(Name.of("generator")),
        name = Name.of("generate"),
        arguments = mapOf(
            Name.of("path") to pathExpr,
            Name.of("field") to ConstructorStatement(
                type = Type.Custom("Wirespec.GeneratorFieldArray"),
                namedArguments = mapOf(
                    Name.of("generate") to Lambda(
                        parameters = lambdaPathParam(depth + 1),
                        body = buildLeafExpr(
                            ref.reference,
                            lambdaPathRef(depth + 1),
                            typeName,
                            annotations,
                            module,
                            depth + 1,
                        ),
                    ),
                ),
            ),
        ),
    )
    is ReferenceWirespec.Dict -> FunctionCall(
        receiver = VariableReference(Name.of("generator")),
        name = Name.of("generate"),
        arguments = mapOf(
            Name.of("path") to pathExpr,
            Name.of("field") to ConstructorStatement(
                type = Type.Custom("Wirespec.GeneratorFieldDict"),
                namedArguments = mapOf(
                    Name.of("generate") to Lambda(
                        parameters = lambdaPathParam(depth + 1),
                        body = buildLeafExpr(
                            ref.reference,
                            lambdaPathRef(depth + 1),
                            typeName,
                            annotations,
                            module,
                            depth + 1,
                        ),
                    ),
                ),
            ),
        ),
    )
    is ReferenceWirespec.Any, is ReferenceWirespec.Unit -> NullLiteral
}

private fun ReferenceWirespec.toGeneratorExpression(
    typeName: String,
    fieldNameStr: String,
    annotations: List<AnnotationWirespec>,
    module: Module?,
): Expression {
    val outerPath = pathPlus(fieldNameStr)

    return if (this.isNullable) {
        // The seeded generator decides null vs invoke-the-thunk; the call site has no `if`.
        FunctionCall(
            receiver = VariableReference(Name.of("generator")),
            name = Name.of("generate"),
            arguments = mapOf(
                Name.of("path") to outerPath,
                Name.of("field") to ConstructorStatement(
                    type = Type.Custom("Wirespec.GeneratorFieldNullable"),
                    namedArguments = mapOf(
                        Name.of("generate") to Lambda(
                            parameters = lambdaPathParam(0),
                            body = buildLeafExpr(this, lambdaPathRef(0), typeName, annotations, module, 0),
                        ),
                    ),
                ),
            ),
        )
    } else {
        buildLeafExpr(this, outerPath, typeName, annotations, module, -1)
    }
}

fun RefinedWirespec.convertToGenerator(): File {
    val generatorName = identifier.toGeneratorName()
    val typeName = identifier.value

    return file(generatorName) {
        namespace(generatorName) {
            function("generate") {
                arg("generator", type("Wirespec.Generator"))
                arg("path", list(string))
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
                                    Name.of("field") to reference.toFieldDescriptor(annotations),
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
                arg("generator", type("Wirespec.Generator"))
                arg("path", list(string))
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
                                    Name.of("field") to ConstructorStatement(
                                        type = Type.Custom("Wirespec.GeneratorFieldEnum"),
                                        namedArguments = mapOf(
                                            Name.of("values") to LiteralList(
                                                values = entries.map { Literal(it, Type.String) },
                                                type = Type.String,
                                            ),
                                            Name.of("annotations") to annotationsToIrList(annotations),
                                            Name.of("type") to ClassReference(Type.Custom(typeName)),
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
                arg("generator", type("Wirespec.Generator"))
                arg("path", list(string))
                returnType(type(typeName))
                assign(
                    "variant",
                    FunctionCall(
                        receiver = VariableReference(Name.of("generator")),
                        name = Name.of("generate"),
                        arguments = mapOf(
                            Name.of("path") to pathPlus("variant"),
                            Name.of("field") to ConstructorStatement(
                                type = Type.Custom("Wirespec.GeneratorFieldUnion"),
                                namedArguments = mapOf(
                                    Name.of("variants") to LiteralList(
                                        values = variantNames.map { Literal(it, Type.String) },
                                        type = Type.String,
                                    ),
                                    Name.of("annotations") to annotationsToIrList(annotations),
                                    Name.of("type") to ClassReference(Type.Custom(typeName)),
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
                                        Name.of("generator") to VariableReference(Name.of("generator")),
                                        Name.of("path") to pathPlus(variantName),
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
