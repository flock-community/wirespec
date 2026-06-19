package community.flock.wirespec.integration.avro.extension

import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.integration.avro.Utils
import community.flock.wirespec.integration.avro.Utils.isEnum
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

/**
 * A single [IrExtension] that appends an Avro schema + converter declaration (`<Type>Avro`) for
 * every record and enum in the spec, next to the model classes produced by the IR emitter.
 *
 * The `<Type>Avro` declaration is built with the IR DSL ([typeAvroFile] / [enumAvroFile]) so the file/namespace/method
 * structure is language-neutral and rendered idiomatically by the Java and Kotlin generators
 * (Java: a class-like interface with `static` methods; Kotlin: an `object`). Only the genuinely
 * language-specific leaves remain as code strings: the `SCHEMA` field initializer and the
 * per-field conversion expressions (`data.x()` vs `data.x`, casts, streams vs `map`, byte
 * handling). Those are produced by a [JavaAvroSource] / [KotlinAvroSource] which reuse the
 * matching [JavaEmitter] / [KotlinEmitter] rendering helpers.
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

private class JavaAvroSource(packageName: PackageName) :
    JavaEmitter(packageName, EmitShared(false)),
    AvroSource {

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

    private fun schemaField(definition: Definition, module: Module) =
        """
        |public static final org.apache.avro.Schema SCHEMA =
        |  new org.apache.avro.Schema.Parser().parse("${schema(definition, module)}");
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
}

private class KotlinAvroSource(packageName: PackageName) :
    KotlinEmitter(packageName, EmitShared(false)),
    AvroSource {

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

    private fun schemaField(definition: Definition, module: Module, explicitType: Boolean): String {
        val declaration = if (explicitType) "val SCHEMA: org.apache.avro.Schema" else "val SCHEMA"
        return "$declaration = org.apache.avro.Schema.Parser().parse(\"${schema(definition, module)}\")"
    }

    private fun toValue(field: Field): String = when (val reference = field.reference) {
        is Reference.Iterable -> "data.${emit(field.identifier)}.map{${reference.reference.value.avroClass()}.to(it)}"
        is Reference.Custom -> "${field.reference.emit().avroClass()}.to(data.${emit(field.identifier)})"
        is Reference.Primitive -> when {
            reference.type == Reference.Primitive.Type.Bytes -> "java.nio.ByteBuffer.wrap(data.${emit(field.identifier)}.toByteArray())"
            else -> "data.${emit(field.identifier)}"
        }

        else -> error("Cannot emit Avro: $reference")
    }

    private fun fromValue(module: Module): (index: Int, field: Field) -> String = { index, field ->
        when (val reference = field.reference) {
            is Reference.Iterable -> "(record.get($index) as java.util.List<org.apache.avro.generic.GenericData.Record>).map{${reference.reference.emit().avroClass()}.from(it)}"
            is Reference.Custom -> when {
                reference.isEnum(module) -> "${field.reference.emit().avroClass()}.from(record.get($index) as org.apache.avro.generic.GenericData.EnumSymbol)"
                else -> "${field.reference.emit().avroClass()}.from(record.get($index) as org.apache.avro.generic.GenericData.Record)"
            }

            is Reference.Primitive -> when (reference.type) {
                is Reference.Primitive.Type.Bytes -> "String((record.get($index) as java.nio.ByteBuffer).array())"
                is Reference.Primitive.Type.String -> "record.get($index)${if (reference.isNullable) "?" else ""}.toString() as ${reference.emit()}"
                else -> "record.get($index) as ${reference.emit()}"
            }

            else -> "record.get($index): ${reference.emit()}"
        }
    }
}

/** The escaped Avro schema JSON for [definition], with nested record references restored. */
private fun schema(packageName: PackageName, definition: Definition, module: Module): String = Utils.emitAvroSchema(packageName, definition, module)
    ?.replace("\\\"<<<<<", "\" + ")
    ?.replace(">>>>>\\\"", "Avro.SCHEMA + \"")
    ?: error("Cannot emit avro: ${definition.identifier.value}")

private fun JavaEmitter.schema(definition: Definition, module: Module): String = schema(packageName, definition, module)
private fun KotlinEmitter.schema(definition: Definition, module: Module): String = schema(packageName, definition, module)

private fun String.avroClass(): String = replace(".model.", ".avro.") + "Avro"
