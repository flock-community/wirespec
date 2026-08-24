package community.flock.wirespec.integration.avro.extension

import arrow.core.escaped
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.converter.avro.AvroJsonEmitter
import community.flock.wirespec.converter.avro.AvroModel
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.Expression
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.IR
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.file
import community.flock.wirespec.ir.extension.IrExtension
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A single [IrExtension] that appends an Avro schema + converter declaration (`<Type>Avro`) for
 * every record and enum in the spec, next to the model classes produced by the IR emitter.
 *
 * The `<Type>Avro` declaration is built with the IR DSL ([typeAvroFile] / [enumAvroFile]) so the file/namespace/method
 * structure is language-neutral and rendered idiomatically by the Java and Kotlin generators
 * (Java: a class-like interface with `static` methods; Kotlin: an `object`). Only the genuinely
 * language-specific leaves remain as code strings: the `SCHEMA` field initializer and the
 * per-field conversion expressions (`data.x()` vs `data.x`, casts, streams vs `map`, byte
 * handling). Those are produced by a [JavaAvroSource] / [KotlinAvroSource] which render the
 * matching Java / Kotlin identifier and type names.
 *
 * The target [language] is supplied by the plugin (the emitter's [FileExtension]) and selects
 * which source renders the language-specific leaves. Register it on a Java or Kotlin
 * [community.flock.wirespec.ir.emit.IrEmitter] running in IR mode (add the `avro-jvm` integration
 * to the plugin classpath and list this class under `extensionClasses`).
 */
class AvroExtension(packageName: PackageName, language: FileExtension) : IrExtension {

    private val source: AvroSource = when (language) {
        FileExtension.Java -> JavaAvroSource(packageName)
        FileExtension.Kotlin -> KotlinAvroSource(packageName)
        else -> error("AvroExtension supports Java and Kotlin targets only, got $language")
    }

    override fun extend(ir: IR, ast: AST): IR {
        val avroFiles = buildList {
            ast.modules.forEach { module ->
                module.statements.forEach { definition ->
                    addAll(source.avroFiles(definition, module))
                }
            }
        }
        val base: List<Element> = ir
        return (base + avroFiles).toNonEmptyListOrNull() ?: ir
    }
}

private const val RECORD = "org.apache.avro.generic.GenericData.Record"
private const val ENUM_SYMBOL = "org.apache.avro.generic.GenericData.EnumSymbol"

/**
 * Builds the `<Type>Avro` declaration for a record: a namespace holding the [schemaField], a
 * `from(record)` that constructs the model from positional [fromArgs], and a `to(data)` that
 * fills a fresh `Record` with the [toPuts] statements.
 */
private fun typeAvroFile(
    packageName: PackageName,
    typeName: String,
    schemaField: String,
    recordConstructor: String,
    fromArgs: List<Pair<String, String>>,
    toPuts: List<String>,
): File = file(Name("${packageName.toDir()}avro/${typeName}Avro")) {
    `package`("${packageName.value}.avro")
    import("${packageName.value}.model", typeName)
    namespace("${typeName}Avro") {
        raw(schemaField)
        function("from", isStatic = true) {
            arg("record", type(RECORD))
            returnType(type(typeName))
            returns(
                ConstructorStatement(
                    type = type(typeName),
                    // Named after the model fields: Java renders them positionally, Kotlin as
                    // named arguments that line up with the generated data-class parameters.
                    namedArguments = fromArgs.associate { (name, value) -> Name.of(name) to (RawExpression(value) as Expression) },
                ),
            )
        }
        function("to", isStatic = true) {
            arg("data", type(typeName))
            returnType(type(RECORD))
            assign("record", RawExpression(recordConstructor))
            toPuts.forEach { raw(it) }
            returns(VariableReference(Name.of("record")))
        }
    }
}

/**
 * Builds the `<Type>Avro` declaration for an enum: a namespace holding the [schemaField] and the
 * single-expression [fromExpr] / [toExpr] converters between the model enum and an `EnumSymbol`.
 */
private fun enumAvroFile(
    packageName: PackageName,
    enumName: String,
    schemaField: String,
    fromExpr: String,
    toExpr: String,
): File = file(Name("${packageName.toDir()}avro/${enumName}Avro")) {
    `package`("${packageName.value}.avro")
    import("${packageName.value}.model", enumName)
    namespace("${enumName}Avro") {
        raw(schemaField)
        function("from", isStatic = true) {
            arg("record", type(ENUM_SYMBOL))
            returnType(type(enumName))
            returns(RawExpression(fromExpr))
        }
        function("to", isStatic = true) {
            arg("data", type(enumName))
            returnType(type(ENUM_SYMBOL))
            returns(RawExpression(toExpr))
        }
    }
}

private interface AvroSource {
    fun avroFiles(definition: Definition, module: Module): List<File>
}

private class JavaAvroSource(private val packageName: PackageName) : AvroSource {

    override fun avroFiles(definition: Definition, module: Module): List<File> = when (definition) {
        is Type -> listOf(
            typeAvroFile(
                packageName = packageName,
                typeName = emit(definition.identifier),
                schemaField = schemaField(definition, module),
                recordConstructor = "new $RECORD(SCHEMA)",
                fromArgs = definition.shape.value.mapIndexed { index, field -> emit(field.identifier) to fromValue(module)(index, field) },
                toPuts = definition.shape.value.mapIndexed { index, field -> "record.put($index, ${toValue(field)})" },
            ),
        )
        is Enum -> listOf(
            enumAvroFile(
                packageName = packageName,
                enumName = emit(definition.identifier),
                schemaField = schemaField(definition, module),
                fromExpr = "${emit(definition.identifier)}.valueOf(record.toString())",
                toExpr = "new $ENUM_SYMBOL(SCHEMA, data.name())",
            ),
        )
        else -> emptyList()
    }

    private fun emit(identifier: Identifier): String = when (identifier) {
        is DefinitionIdentifier -> identifier.value.sanitizeSymbol()
        is FieldIdentifier -> identifier.value.sanitizeSymbol().let { if (it in reservedKeywords) "_$it" else it }
    }

    private fun String.sanitizeSymbol() = this
        .split(".", " ", "-")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .asSequence()
        .filter { it.isLetterOrDigit() || it in listOf('_') }
        .joinToString("")
        .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }

    private fun Reference.emit(): String = emitType()
        .let { if (isNullable) "java.util.Optional<$it>" else it }

    private fun Reference.emitType(): String = when (this) {
        is Reference.Dict -> "java.util.Map<String, ${reference.emit()}>"
        is Reference.Iterable -> "java.util.List<${reference.emit()}>"
        is Reference.Unit -> "void"
        is Reference.Any -> "Object"
        is Reference.Custom -> value
        is Reference.Primitive -> when (val t = type) {
            is Reference.Primitive.Type.String -> "String"
            is Reference.Primitive.Type.Integer -> when (t.precision) {
                Reference.Primitive.Type.Precision.P32 -> "Integer"
                Reference.Primitive.Type.Precision.P64 -> "Long"
            }

            is Reference.Primitive.Type.Number -> when (t.precision) {
                Reference.Primitive.Type.Precision.P32 -> "Float"
                Reference.Primitive.Type.Precision.P64 -> "Double"
            }

            is Reference.Primitive.Type.Boolean -> "Boolean"
            is Reference.Primitive.Type.Bytes -> "byte[]"
        }
    }

    private fun Reference.emitRoot(): String = when (this) {
        is Reference.Dict -> reference.emitRoot()
        is Reference.Iterable -> reference.emitRoot()
        is Reference.Unit -> "void"
        else -> emitType()
    }

    private fun schemaField(definition: Definition, module: Module) =
        """
        |public static final org.apache.avro.Schema SCHEMA =
        |  new org.apache.avro.Schema.Parser().parse("${schema(packageName, definition, module)}");
        |
        """.trimMargin()

    private fun toValue(field: Field): String = when (val reference = field.reference) {
        is Reference.Iterable -> "data.${emit(field.identifier)}().stream().map(it -> ${reference.reference.value.avroClass()}.to(it)).toList()"
        is Reference.Custom -> "${field.reference.emit().avroClass()}.to(data.${emit(field.identifier)}())"
        is Reference.Primitive -> when (reference.type) {
            is Reference.Primitive.Type.Bytes -> "java.nio.ByteBuffer.wrap(data.${emit(field.identifier)}())"
            else -> "data.${emit(field.identifier)}()${if (reference.isNullable) ".orElse(null)" else ""}"
        }

        else -> TODO()
    }

    private fun fromValue(module: Module): (index: Int, field: Field) -> String = { index, field ->
        when (val reference = field.reference) {
            is Reference.Iterable -> "((java.util.List<org.apache.avro.generic.GenericData.Record>) record.get($index)).stream().map(it -> ${reference.reference.emitRoot().avroClass()}.from(it)).toList()"
            is Reference.Custom -> when {
                reference.isNullable -> "(${reference.emit()}) java.util.Optional.ofNullable((${field.reference.emitRoot()}) record.get($index))"
                reference.isEnum(module) -> "${field.reference.emit().avroClass()}.from((org.apache.avro.generic.GenericData.EnumSymbol) record.get($index))"
                else -> "${field.reference.emit().avroClass()}.from((org.apache.avro.generic.GenericData.Record) record.get($index))"
            }

            is Reference.Primitive -> when {
                reference.isNullable -> "(${reference.emit()}) java.util.Optional.ofNullable((${field.reference.emitRoot()}) record.get($index))"
                reference.type == Reference.Primitive.Type.Bytes -> "(${reference.emit()}) ((java.nio.ByteBuffer) record.get($index)).array()"
                reference.type == Reference.Primitive.Type.String(null) -> "(${reference.emit()}) record.get($index).toString()"
                else -> "(${reference.emit()}) record.get($index)"
            }

            else -> "(${reference.emit()}) record.get($index)"
        }
    }

    companion object {
        private val reservedKeywords = setOf(
            "abstract", "continue", "for", "new", "switch",
            "assert", "default", "goto", "package", "synchronized",
            "boolean", "do", "if", "private", "this",
            "break", "double", "implements", "protected", "throw",
            "byte", "else", "import", "public", "throws",
            "case", "enum", "instanceof", "return", "transient",
            "catch", "extends", "int", "short", "try",
            "char", "final", "interface", "static", "void",
            "class", "finally", "long", "strictfp", "volatile",
            "const", "float", "native", "super", "while",
            "true", "false",
        )
    }
}

private class KotlinAvroSource(private val packageName: PackageName) : AvroSource {

    override fun avroFiles(definition: Definition, module: Module): List<File> = when (definition) {
        is Type -> listOf(
            typeAvroFile(
                packageName = packageName,
                typeName = emit(definition.identifier),
                schemaField = schemaField(definition, module, explicitType = false),
                recordConstructor = "$RECORD(SCHEMA)",
                fromArgs = definition.shape.value.mapIndexed { index, field -> emit(field.identifier) to fromValue(module)(index, field) },
                toPuts = definition.shape.value.mapIndexed { index, field -> "record.put($index, ${toValue(field)})" },
            ),
        )
        is Enum -> listOf(
            enumAvroFile(
                packageName = packageName,
                enumName = emit(definition.identifier),
                schemaField = schemaField(definition, module, explicitType = true),
                fromExpr = "${emit(definition.identifier)}.valueOf(record.toString())",
                toExpr = "$ENUM_SYMBOL(SCHEMA, data.name)",
            ),
        )
        else -> emptyList()
    }

    private fun emit(identifier: Identifier): String = when (identifier) {
        is DefinitionIdentifier -> identifier.sanitize()
        is FieldIdentifier -> identifier.sanitize().let { if (it in reservedKeywords) it.addBackticks() else it }
    }

    private fun Identifier.sanitize() = value
        .split(".", " ")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .asSequence()
        .filter { it.isLetterOrDigit() || it in listOf('_') }
        .joinToString("")
        .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }

    private fun Reference.emit(): String = when (this) {
        is Reference.Dict -> "Map<String, ${reference.emit()}>"
        is Reference.Iterable -> "List<${reference.emit()}>"
        is Reference.Unit -> "Unit"
        is Reference.Any -> "Any"
        is Reference.Custom -> value
        is Reference.Primitive -> when (val t = type) {
            is Reference.Primitive.Type.String -> "String"
            is Reference.Primitive.Type.Integer -> when (t.precision) {
                Reference.Primitive.Type.Precision.P32 -> "Int"
                Reference.Primitive.Type.Precision.P64 -> "Long"
            }

            is Reference.Primitive.Type.Number -> when (t.precision) {
                Reference.Primitive.Type.Precision.P32 -> "Float"
                Reference.Primitive.Type.Precision.P64 -> "Double"
            }

            is Reference.Primitive.Type.Boolean -> "Boolean"
            is Reference.Primitive.Type.Bytes -> "ByteArray"
        }
    }.let { if (isNullable) "$it?" else it }

    /** The `<Type>Avro` object for [reference], named after the type itself, never its nullability. */
    private fun avroObject(reference: Reference): String = reference.copy(isNullable = false).emit().avroClass()

    private fun schemaField(definition: Definition, module: Module, explicitType: Boolean): String {
        val declaration = if (explicitType) "val SCHEMA: org.apache.avro.Schema" else "val SCHEMA"
        return "$declaration = org.apache.avro.Schema.Parser().parse(\"${schema(packageName, definition, module)}\")"
    }

    private fun toValue(field: Field): String {
        val value = "data.${emit(field.identifier)}"
        // A nullable field carries the null straight through to the record; the element
        // conversions below only ever run on a present value.
        val access = if (field.reference.isNullable) "$value?" else value
        return when (val reference = field.reference) {
            // Avro arrays and maps of primitives need no per-element conversion: the model
            // already holds the very types the writer expects.
            is Reference.Iterable -> when (reference.reference) {
                is Reference.Custom -> "$access.map{${avroObject(reference.reference)}.to(it)}"
                else -> value
            }
            is Reference.Dict -> when (reference.reference) {
                is Reference.Custom -> "$access.mapValues{${avroObject(reference.reference)}.to(it.value)}"
                else -> value
            }
            is Reference.Custom -> when {
                field.reference.isNullable -> "$access.let{${avroObject(reference)}.to(it)}"
                else -> "${avroObject(reference)}.to($value)"
            }
            is Reference.Primitive -> when {
                // `bytes` is a ByteArray in the model and a ByteBuffer on the wire.
                reference.type == Reference.Primitive.Type.Bytes && reference.isNullable ->
                    "$access.let{java.nio.ByteBuffer.wrap(it)}"
                reference.type == Reference.Primitive.Type.Bytes -> "java.nio.ByteBuffer.wrap($value)"
                else -> value
            }

            else -> error("Cannot emit Avro: $reference")
        }
    }

    private fun fromValue(module: Module): (index: Int, field: Field) -> String = { index, field ->
        val reference = field.reference
        val get = "record.get($index)"
        // `?` after the cast keeps a null field null; the conversions then run under `?.`.
        val orNull = if (reference.isNullable) "?" else ""
        when (reference) {
            is Reference.Iterable ->
                "($get as kotlin.collections.List<*>$orNull)$orNull.map{${element(module, reference.reference, "it")}}"
            is Reference.Dict ->
                "($get as kotlin.collections.Map<*, *>$orNull)$orNull.entries$orNull.associate{it.key.toString() to ${element(module, reference.reference, "it.value")}}"
            is Reference.Custom -> {
                // An enum arrives as an EnumSymbol rather than a Record, so the cast that guards
                // the null has to name the carrier the value actually has.
                val carrier = if (reference.isEnum(module)) ENUM_SYMBOL else RECORD
                when {
                    reference.isNullable -> "($get as $carrier?)?.let{${avroObject(reference)}.from(it)}"
                    else -> element(module, reference, get)
                }
            }

            is Reference.Primitive -> when (reference.type) {
                is Reference.Primitive.Type.Bytes -> "($get as java.nio.ByteBuffer$orNull)$orNull.array()"
                is Reference.Primitive.Type.String -> "$get$orNull.toString() as ${reference.emit()}"
                else -> "$get as ${reference.emit()}"
            }

            else -> error("Cannot emit Avro: $reference")
        }
    }

    /**
     * Reads a single Avro value held in [value] back into the model type [reference] — the element
     * of an array or map, or a field read straight off the record. Avro hands strings back as
     * `Utf8`, so anything string-shaped goes through `toString()` rather than a cast.
     */
    private fun element(module: Module, reference: Reference, value: String): String = when (reference) {
        is Reference.Custom -> when {
            reference.isEnum(module) -> "${avroObject(reference)}.from($value as $ENUM_SYMBOL)"
            else -> "${avroObject(reference)}.from($value as $RECORD)"
        }
        is Reference.Primitive -> when (reference.type) {
            is Reference.Primitive.Type.String -> "$value.toString()"
            else -> "$value as ${reference.emit()}"
        }
        else -> error("Cannot emit Avro element: $reference")
    }

    companion object {
        private val reservedKeywords = setOf(
            "as", "break", "class", "continue", "do",
            "else", "false", "for", "fun", "if",
            "in", "interface", "internal", "is", "null",
            "object", "open", "package", "return", "super",
            "this", "throw", "true", "try", "typealias",
            "typeof", "val", "var", "when", "while", "private", "public",
        )
    }
}

/** The escaped Avro schema JSON for [definition], with nested record references restored. */
private fun schema(packageName: PackageName, definition: Definition, module: Module): String = emitAvroSchema(packageName, definition, module)
    ?.replace("\\\"<<<<<", "\" + ")
    ?.replace(">>>>>\\\"", "Avro.SCHEMA + \"")
    ?: error("Cannot emit avro: ${definition.identifier.value}")

private fun String.avroClass(): String = replace(".model.", ".avro.") + "Avro"

/**
 * The Avro schema for [definition] alone. Emitting per definition rather than picking one out of
 * the whole module keeps every schema self-contained: nested records are written out in full the
 * first time they appear, and the seeded name makes a type that refers back to itself — directly
 * or through a nested record — resolve by name instead of trying to inline itself forever.
 */
private fun emitAvroSchema(packageName: PackageName, definition: Definition, module: Module) = with(AvroJsonEmitter) {
    when (definition) {
        is Type ->
            definition
                .emit(module, mutableListOf(definition.identifier.value))
                .copy(namespace = packageName.value)
        is Enum -> definition.emit()
        else -> null
    }
}
    ?.flatten(mutableSetOf())
    ?.let { Json.encodeToString(it) }
    ?.escaped()

private fun Reference.isEnum(module: Module): Boolean = module.statements
    .filterIsInstance<Enum>()
    .any { it.identifier.value == this.value }

private val AVRO_PRIMITIVES = setOf("boolean", "int", "long", "float", "double", "bytes", "string", "null")

/**
 * Marks every name this schema does not define itself, so [schema] can splice in the emitting
 * object's `SCHEMA` for it. [defined] collects the names defined along the way — a record or enum
 * written out in full here, or a name already spliced in — and a repeat of one of those stays a
 * bare Avro name, which is both how Avro deduplicates and the only way to write a recursive type.
 */
private fun AvroModel.Type.flatten(defined: MutableSet<String>): AvroModel.Type = when (this) {
    is AvroModel.RecordType -> {
        // Registered before the fields are walked, so a field referring back to this record finds it.
        defined.add(name)
        this
            .copy(
                fields = fields
                    .map { field ->
                        field.copy(
                            type = AvroModel.TypeList(
                                field.type
                                    .map { it.flatten(defined) },
                            ),
                        )
                    },
            )
    }

    is AvroModel.ArrayType -> this.copy(items = items.flatten(defined))
    is AvroModel.EnumType -> this.also { defined.add(name) }
    is AvroModel.LogicalType -> this
    is AvroModel.SimpleType -> when {
        value in AVRO_PRIMITIVES || value in defined -> this
        else -> {
            defined.add(value)
            this.copy(value = "<<<<<$value>>>>>")
        }
    }

    is AvroModel.MapType -> this.copy(values = values.flatten(defined))
    is AvroModel.UnionType -> this.copy(type = AvroModel.TypeList(type.map { it.flatten(defined) }))
}
