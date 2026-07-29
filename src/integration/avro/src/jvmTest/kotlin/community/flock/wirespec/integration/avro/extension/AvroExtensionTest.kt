package community.flock.wirespec.integration.avro.extension

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.ir.extension.ExtendingIrEmitter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AvroExtensionTest {

    private val packageName = PackageName("packageName")

    private val javaEmitter = ExtendingIrEmitter(
        JavaIrEmitter(packageName, EmitShared(true)),
        listOf(AvroExtension(packageName, FileExtension.Java)),
    )

    private val kotlinEmitter = ExtendingIrEmitter(
        KotlinIrEmitter(packageName, EmitShared(true)),
        listOf(AvroExtension(packageName, FileExtension.Kotlin)),
    )

    private fun List<Emitted>.avro(file: String): String? = find { it.file == file }?.result

    private val type = Type(
        comment = null,
        annotations = emptyList(),
        identifier = DefinitionIdentifier("Identifier"),
        shape = Type.Shape(
            listOf(
                Field(
                    identifier = FieldIdentifier(name = "name"),
                    annotations = emptyList(),
                    reference = Reference.Primitive(
                        type = Reference.Primitive.Type.String(null),
                        isNullable = false,
                    ),
                ),
            ),
        ),
        extends = emptyList(),
    )

    private val enum = Enum(
        comment = null,
        annotations = emptyList(),
        identifier = DefinitionIdentifier("Identifier"),
        entries = setOf("ONE", "TWO", "THREE"),
    )

    private fun field(name: String, reference: Reference) = Field(
        identifier = FieldIdentifier(name = name),
        annotations = emptyList(),
        reference = reference,
    )

    private fun string(isNullable: Boolean = false) =
        Reference.Primitive(type = Reference.Primitive.Type.String(null), isNullable = isNullable)

    private val nested = Type(
        comment = null,
        annotations = emptyList(),
        identifier = DefinitionIdentifier("Nested"),
        shape = Type.Shape(listOf(field("name", string()))),
        extends = emptyList(),
    )

    /** Maps, arrays of primitives and nullable records — none of which the flat [type] covers. */
    private val collections = Type(
        comment = null,
        annotations = emptyList(),
        identifier = DefinitionIdentifier("Collections"),
        shape = Type.Shape(
            listOf(
                field("externalIds", Reference.Dict(reference = string(), isNullable = false)),
                field("text", Reference.Dict(reference = Reference.Custom("Nested", false), isNullable = false)),
                field("gtins", Reference.Iterable(reference = string(), isNullable = false)),
                field("children", Reference.Iterable(reference = Reference.Custom("Nested", false), isNullable = true)),
                field("update", Reference.Custom("Nested", isNullable = true)),
            ),
        ),
        extends = emptyList(),
    )

    @Test
    fun emitKotlinCollections() {
        val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(collections, nested))))
        val emitted = kotlinEmitter.emit(ast, noLogger).avro("packageName/avro/CollectionsAvro.kt").orEmpty()

        // Maps and arrays of primitives already hold the types the writer wants and pass through
        // untouched; the record-valued ones convert per entry. A nullable collection or record
        // keeps its null rather than being dereferenced.
        assertContains(emitted, "record.put(0, data.externalIds)")
        assertContains(emitted, "record.put(1, data.text.mapValues{NestedAvro.to(it.value)})")
        assertContains(emitted, "record.put(2, data.gtins)")
        assertContains(emitted, "record.put(3, data.children?.map{NestedAvro.to(it)})")
        assertContains(emitted, "record.put(4, data.update?.let{NestedAvro.to(it)})")

        // Avro hands strings back as Utf8, and map keys with them, so both go through toString().
        assertContains(emitted, "externalIds = (record.get(0) as kotlin.collections.Map<*, *>).entries.associate{it.key.toString() to it.value.toString()}")
        assertContains(emitted, "text = (record.get(1) as kotlin.collections.Map<*, *>).entries.associate{it.key.toString() to NestedAvro.from(it.value as org.apache.avro.generic.GenericData.Record)}")
        assertContains(emitted, "gtins = (record.get(2) as kotlin.collections.List<*>).map{it.toString()}")
        assertContains(emitted, "children = (record.get(3) as kotlin.collections.List<*>?)?.map{NestedAvro.from(it as org.apache.avro.generic.GenericData.Record)}")
        assertContains(emitted, "update = (record.get(4) as org.apache.avro.generic.GenericData.Record?)?.let{NestedAvro.from(it)}")

        // The schema keeps the map structure rather than failing to flatten it.
        assertContains(emitted, """{\"type\":\"map\",\"values\":\"string\"}""")
    }

    /** A taxonomy node holding its own parents — the shape that used to inline itself forever. */
    private val recursive = Type(
        comment = null,
        annotations = emptyList(),
        identifier = DefinitionIdentifier("Node"),
        shape = Type.Shape(
            listOf(
                field("name", string()),
                field("parents", Reference.Iterable(reference = Reference.Custom("Node", false), isNullable = false)),
                field("nested", Reference.Custom("Nested", isNullable = false)),
                field("alsoNested", Reference.Custom("Nested", isNullable = false)),
            ),
        ),
        extends = emptyList(),
    )

    @Test
    fun emitKotlinRecursiveSchema() {
        val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(recursive, nested))))
        val emitted = kotlinEmitter.emit(ast, noLogger).avro("packageName/avro/NodeAvro.kt").orEmpty()

        // The self reference is a bare Avro name: the record is already defined by the time the
        // field is read, so there is nothing left to inline.
        assertContains(emitted, """{\"name\":\"parents\",\"type\":{\"type\":\"array\",\"items\":\"Node\"}}""")
        // A nested record is written out in full once...
        assertContains(emitted, """{\"name\":\"nested\",\"type\":{\"type\":\"record\",\"name\":\"Nested\"""")
        // ...and referred to by name after that, since Avro forbids defining a name twice.
        assertContains(emitted, """{\"name\":\"alsoNested\",\"type\":\"Nested\"}""")
        // Nothing is spliced in from another object, so SCHEMA is a single parsed literal.
        org.junit.jupiter.api.Assertions.assertFalse(
            emitted.contains("NodeAvro.SCHEMA"),
            "NodeAvro.SCHEMA must not reference itself:\n$emitted",
        )
    }

    private fun assertContains(actual: String, expected: String) = org.junit.jupiter.api.Assertions.assertTrue(
        actual.contains(expected),
        "expected to find:\n  $expected\nin:\n$actual",
    )

    @Test
    fun emitJavaType() {
        val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(type))))
        val expected =
            """
            |package packageName.avro;
            |import packageName.model.Identifier;
            |public interface IdentifierAvro {
            |  public static final org.apache.avro.Schema SCHEMA =
            |    new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Identifier\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}");
            |  public static Identifier from(org.apache.avro.generic.GenericData.Record record) {
            |    return new Identifier((String) record.get(0).toString());
            |  }
            |  public static org.apache.avro.generic.GenericData.Record to(Identifier data) {
            |    final var record = new org.apache.avro.generic.GenericData.Record(SCHEMA);
            |    record.put(0, data.name());
            |    return record;
            |  }
            |}
            |
            """.trimMargin()
        assertEquals(expected, javaEmitter.emit(ast, noLogger).avro("packageName/avro/IdentifierAvro.java"))
    }

    @Test
    fun emitJavaEnum() {
        val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(enum))))
        val expected =
            """
            |package packageName.avro;
            |import packageName.model.Identifier;
            |public interface IdentifierAvro {
            |  public static final org.apache.avro.Schema SCHEMA =
            |    new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"Identifier\",\"symbols\":[\"ONE\",\"TWO\",\"THREE\"]}");
            |  public static Identifier from(org.apache.avro.generic.GenericData.EnumSymbol record) {
            |    return Identifier.valueOf(record.toString());
            |  }
            |  public static org.apache.avro.generic.GenericData.EnumSymbol to(Identifier data) {
            |    return new org.apache.avro.generic.GenericData.EnumSymbol(SCHEMA, data.name());
            |  }
            |}
            |
            """.trimMargin()
        assertEquals(expected, javaEmitter.emit(ast, noLogger).avro("packageName/avro/IdentifierAvro.java"))
    }

    @Test
    fun emitKotlinType() {
        val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(type))))
        val expected =
            """
            |package packageName.avro
            |import packageName.model.Identifier
            |object IdentifierAvro {
            |  val SCHEMA = org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Identifier\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}")
            |  fun from(record: org.apache.avro.generic.GenericData.Record): Identifier =
            |    Identifier(name = record.get(0).toString() as String)
            |  fun to(data: Identifier): org.apache.avro.generic.GenericData.Record {
            |    val record = org.apache.avro.generic.GenericData.Record(SCHEMA)
            |    record.put(0, data.name)
            |    return record
            |  }
            |}
            |
            """.trimMargin()
        assertEquals(expected, kotlinEmitter.emit(ast, noLogger).avro("packageName/avro/IdentifierAvro.kt"))
    }

    @Test
    fun emitKotlinEnum() {
        val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(enum))))
        val expected =
            """
            |package packageName.avro
            |import packageName.model.Identifier
            |object IdentifierAvro {
            |  val SCHEMA: org.apache.avro.Schema = org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"Identifier\",\"symbols\":[\"ONE\",\"TWO\",\"THREE\"]}")
            |  fun from(record: org.apache.avro.generic.GenericData.EnumSymbol): Identifier =
            |    Identifier.valueOf(record.toString())
            |  fun to(data: Identifier): org.apache.avro.generic.GenericData.EnumSymbol =
            |    org.apache.avro.generic.GenericData.EnumSymbol(SCHEMA, data.name)
            |}
            |
            """.trimMargin()
        assertEquals(expected, kotlinEmitter.emit(ast, noLogger).avro("packageName/avro/IdentifierAvro.kt"))
    }
}
