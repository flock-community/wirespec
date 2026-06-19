package community.flock.wirespec.integration.avro.extension

import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.spacer
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.integration.avro.Utils
import community.flock.wirespec.integration.avro.Utils.isEnum
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.IR
import community.flock.wirespec.ir.core.Import
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Package
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.extension.IrExtension

/**
 * A single [IrExtension] that appends an Avro schema + converter declaration (`<Type>Avro`) for
 * every record and enum in the spec, next to the model classes produced by the IR emitter.
 *
 * It works for **both** the Java and Kotlin IR emitters: the Avro converter bodies are
 * language-specific (`data.field()` vs `model.field`, casts, `var`/`val`, …), so the extension
 * detects the target language from the IR — the shared `Wirespec` package emitted/imported by
 * the model files is `$DEFAULT_SHARED_PACKAGE_STRING.java` for Java and
 * `$DEFAULT_SHARED_PACKAGE_STRING.kotlin` for Kotlin — and renders the matching source.
 *
 * Register it on a Java or Kotlin [community.flock.wirespec.ir.emit.IrEmitter] running in IR mode
 * (add the `avro-jvm` integration to the plugin classpath and list this class under
 * `extensionClasses`). Each generated model gains a `<Type>Avro` file in the `<package>.avro`
 * sub-package that parses the Avro `Schema` at load time and converts between the generated model
 * and Avro `GenericData.Record` / `EnumSymbol` values.
 */
class AvroExtension(packageName: PackageName) : IrExtension {

    private val java = JavaAvroSource(packageName)
    private val kotlin = KotlinAvroSource(packageName)

    override fun extend(ir: IR, ast: AST): IR {
        val source = ir.detectSource()
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

    private fun IR.detectSource(): AvroSource {
        val paths = filterIsInstance<File>()
            .flatMap { it.elements }
            .mapNotNull {
                when (it) {
                    is Import -> it.path
                    is Package -> it.path
                    else -> null
                }
            }
        return when {
            paths.any { it == "$DEFAULT_SHARED_PACKAGE_STRING.kotlin" } -> kotlin
            else -> java
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
        is Type -> listOf(avroFile(definition.identifier, emitAvroType(definition, module)))
        is Enum -> listOf(avroFile(definition.identifier, emitAvroEnum(definition, module)))
        else -> emptyList()
    }

    private fun avroFile(identifier: Identifier, source: String): File =
        File(Name("${packageName.toDir()}avro/${emit(identifier)}Avro"), listOf(RawElement(source)))

    private fun emitAvroSchema(type: Definition, module: Module) = Utils.emitAvroSchema(packageName, type, module)
        ?.replace("\\\"<<<<<", "\" + ")
        ?.replace(">>>>>\\\"", "Avro.SCHEMA + \"")
        ?: error("Cannot emit avro: ${type.identifier}")

    private fun emitAvroType(type: Type, module: Module) = """
        |package ${packageName.value}.avro;
        |
        |import ${packageName.value}.model.${emit(type.identifier)};
        |
        |public class ${emit(type.identifier)}Avro {
        |
        |  public static final org.apache.avro.Schema SCHEMA =
        |    new org.apache.avro.Schema.Parser().parse("${emitAvroSchema(type, module)}");
        |
        |  public static ${emit(type.identifier)} from(org.apache.avro.generic.GenericData.Record record) {
        |    return new ${emit(type.identifier)}(
        |${type.shape.value.mapIndexed(emitFrom(module)).joinToString(",\n").spacer(3)}
        |    );
        |  }
        |
        |  public static org.apache.avro.generic.GenericData.Record to(${emit(type.identifier)} data) {
        |    var record = new org.apache.avro.generic.GenericData.Record(SCHEMA);
        |${type.shape.value.mapIndexed(emitTo).joinToString("\n").spacer(3)}
        |    return record;
        |  }
        |}
    """.trimMargin()

    private fun emitAvroEnum(enum: Enum, module: Module) = """
        |package ${packageName.value}.avro;
        |
        |import ${packageName.value}.model.${emit(enum.identifier)};
        |
        |public class ${emit(enum.identifier)}Avro {
        |
        |  public static final org.apache.avro.Schema SCHEMA =
        |    new org.apache.avro.Schema.Parser().parse("${emitAvroSchema(enum, module)}");
        |
        |  public static ${emit(enum.identifier)} from(org.apache.avro.generic.GenericData.EnumSymbol record) {
        |    return ${emit(enum.identifier)}.valueOf(record.toString());
        |  }
        |
        |  public static org.apache.avro.generic.GenericData.EnumSymbol to(${emit(enum.identifier)} data) {
        |    return new org.apache.avro.generic.GenericData.EnumSymbol(SCHEMA, data.name());
        |  }
        |}
    """.trimMargin()

    private val emitTo: (index: Int, field: Field) -> String = { index, field ->
        when (val reference = field.reference) {
            is Reference.Iterable -> "record.put($index, data.${emit(field.identifier)}().stream().map(it -> ${reference.reference.value.avroClass()}.to(it)).toList());"
            is Reference.Custom -> "record.put($index, ${field.reference.emit().avroClass()}.to(data.${emit(field.identifier)}()));"
            is Reference.Primitive -> when (reference.type) {
                is Reference.Primitive.Type.Bytes -> "record.put($index, java.nio.ByteBuffer.wrap(data.${emit(field.identifier)}()));"
                else -> "record.put($index, data.${emit(field.identifier)}()${if (reference.isNullable) ".orElse(null)" else ""});"
            }

            else -> TODO()
        }
    }

    private val emitFrom: (module: Module) -> (index: Int, field: Field) -> String =
        { module ->
            { index, field ->
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

    private fun String.avroClass(): String = replace(".model.", ".avro.") + "Avro"
}

private class KotlinAvroSource(packageName: PackageName) :
    KotlinEmitter(packageName, EmitShared(false)),
    AvroSource {

    override fun avroFiles(definition: Definition, module: Module): List<File> = when (definition) {
        is Type -> listOf(avroFile(definition.identifier, emitAvroType(definition, module)))
        is Enum -> listOf(avroFile(definition.identifier, emitAvroEnum(definition, module)))
        else -> emptyList()
    }

    private fun avroFile(identifier: Identifier, source: String): File =
        File(Name("${packageName.toDir()}avro/${emit(identifier)}Avro"), listOf(RawElement(source)))

    private fun emitAvroSchema(type: Definition, module: Module) = Utils.emitAvroSchema(packageName, type, module)
        ?.replace("\\\"<<<<<", "\" + ")
        ?.replace(">>>>>\\\"", "Avro.SCHEMA + \"")
        ?: error("Cannot emit avro: ${type.identifier.value}")

    private fun emitAvroType(type: Type, module: Module) = """
        |package ${packageName.value}.avro
        |
        |import ${packageName.value}.model.${emit(type.identifier)}
        |
        |object ${emit(type.identifier)}Avro {
        |  val SCHEMA = org.apache.avro.Schema.Parser().parse("${emitAvroSchema(type, module)}")
        |
        |  @JvmStatic
        |  fun from(record: org.apache.avro.generic.GenericData.Record): ${emit(type.identifier)} {
        |    return ${emit(type.identifier)}(
        |      ${type.shape.value.mapIndexed(emitFrom(module)).joinToString(",\n${Spacer(5)}")}
        |    )
        |  }
        |
        |  @JvmStatic
        |  fun to(model: ${emit(type.identifier)} ): org.apache.avro.generic.GenericData.Record {
        |    val record = org.apache.avro.generic.GenericData.Record(SCHEMA)
        |    ${type.shape.value.mapIndexed(emitTo).joinToString("\n${Spacer(4)}")}
        |    return record
        |  }
        |
        |}
    """.trimMargin()

    private fun emitAvroEnum(enum: Enum, module: Module) = """
        |package ${packageName.value}.avro
        |
        |import ${packageName.value}.model.${emit(enum.identifier)}
        |
        |object ${emit(enum.identifier)}Avro {
        |
        |  val SCHEMA: org.apache.avro.Schema = org.apache.avro.Schema.Parser().parse("${emitAvroSchema(enum, module)}")
        |
        |  @JvmStatic
        |  fun from(record: org.apache.avro.generic.GenericData.EnumSymbol): ${emit(enum.identifier)} {
        |    return ${emit(enum.identifier)}.valueOf(record.toString())
        |  }
        |
        |  @JvmStatic
        |  fun to(model: ${emit(enum.identifier)}): org.apache.avro.generic.GenericData.EnumSymbol {
        |    return org.apache.avro.generic.GenericData.EnumSymbol(SCHEMA, model.name)
        |  }
        |
        |}
    """.trimMargin()

    private val emitTo: (index: Int, field: Field) -> String = { index, field ->
        when (val reference = field.reference) {
            is Reference.Iterable -> "record.put($index, model.${emit(field.identifier)}.map{${reference.reference.value.avroClass()}.to(it)})"
            is Reference.Custom -> "record.put($index, ${field.reference.emit().avroClass()}.to(model.${emit(field.identifier)}))"
            is Reference.Primitive -> when {
                reference.type == Reference.Primitive.Type.Bytes -> "record.put($index, java.nio.ByteBuffer.wrap(model.${emit(field.identifier)}.toByteArray()))"
                else -> "record.put($index, model.${emit(field.identifier)})"
            }
            else -> error("Cannot emit Avro: $reference")
        }
    }

    private val emitFrom: (module: Module) -> (index: Int, field: Field) -> String =
        { module ->
            { index, field ->
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

    private fun String.avroClass(): String = replace(".model.", ".avro.") + "Avro"
}
