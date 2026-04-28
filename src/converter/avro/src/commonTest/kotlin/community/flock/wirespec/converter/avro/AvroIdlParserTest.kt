package community.flock.wirespec.converter.avro

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AvroIdlParserTest {

    private fun parse(source: String) = AvroIdlParser.parse(ModuleContent(FileUri("test.ws"), source), true)

    private fun loadResource(name: String): String = SystemFileSystem.source(Path("src/commonTest/resources/$name")).buffered().readString()

    @Test
    fun parseSimpleProtocol() {
        val ast = parse(loadResource("simple.avdl"))
        val statements = ast.modules.flatMap { it.statements }.toList()
        statements.size shouldBe 1
        val pet = statements.first()
        pet.shouldBeInstanceOf<Type>()
        pet.identifier.value shouldBe "Pet"
        pet.shape.value.size shouldBe 3
        pet.shape.value.map { it.identifier.value } shouldContainExactly listOf("name", "age", "vaccinated")
    }

    @Test
    fun parseSimpleProtocolFieldTypes() {
        val ast = parse(loadResource("simple.avdl"))
        val pet = ast.modules.first().statements.first() as Type
        pet.shape.value[0].reference shouldBe Reference.Primitive(
            type = Reference.Primitive.Type.String(null),
            isNullable = false,
        )
        pet.shape.value[1].reference shouldBe Reference.Primitive(
            type = Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P32, null),
            isNullable = false,
        )
        pet.shape.value[2].reference shouldBe Reference.Primitive(
            type = Reference.Primitive.Type.Boolean,
            isNullable = false,
        )
    }

    @Test
    fun parseInlineSimpleProtocol() {
        val source = """
            protocol Test {
                record Item {
                    long id;
                    double price;
                    bytes payload;
                    float ratio;
                }
            }
        """.trimIndent()
        val ast = parse(source)
        val item = ast.modules.first().statements.first() as Type
        item.shape.value[0].reference shouldBe Reference.Primitive(
            type = Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P64, null),
            isNullable = false,
        )
        item.shape.value[1].reference shouldBe Reference.Primitive(
            type = Reference.Primitive.Type.Number(Reference.Primitive.Type.Precision.P64, null),
            isNullable = false,
        )
        item.shape.value[2].reference shouldBe Reference.Primitive(
            type = Reference.Primitive.Type.Bytes,
            isNullable = false,
        )
        item.shape.value[3].reference shouldBe Reference.Primitive(
            type = Reference.Primitive.Type.Number(Reference.Primitive.Type.Precision.P32, null),
            isNullable = false,
        )
    }

    @Test
    fun parseNullableUnion() {
        val ast = parse(loadResource("nullable.avdl"))
        val user = ast.modules.first().statements.first() as Type
        user.shape.value.map { it.identifier.value } shouldContainExactly listOf("id", "nickname", "email", "age")

        user.shape.value[0].reference.isNullable shouldBe false
        user.shape.value[1].reference.isNullable shouldBe true
        user.shape.value[2].reference.isNullable shouldBe true
        user.shape.value[3].reference.isNullable shouldBe true

        user.shape.value[1].reference shouldBe Reference.Primitive(
            type = Reference.Primitive.Type.String(null),
            isNullable = true,
        )
        user.shape.value[3].reference shouldBe Reference.Primitive(
            type = Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P32, null),
            isNullable = true,
        )
    }

    @Test
    fun parseQuestionMarkSyntax() {
        val source = """
            protocol Test {
                record Profile {
                    string? bio;
                    int? followers;
                }
            }
        """.trimIndent()
        val ast = parse(source)
        val profile = ast.modules.first().statements.first() as Type
        profile.shape.value.forEach { it.reference.isNullable shouldBe true }
    }

    @Test
    fun parseArrayAndMap() {
        val ast = parse(loadResource("collections.avdl"))
        val product = ast.modules.first().statements.first() as Type
        product.shape.value[1].reference shouldBe Reference.Iterable(
            reference = Reference.Primitive(type = Reference.Primitive.Type.String(null), isNullable = false),
            isNullable = false,
        )
        product.shape.value[2].reference shouldBe Reference.Dict(
            reference = Reference.Primitive(
                type = Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P32, null),
                isNullable = false,
            ),
            isNullable = false,
        )
    }

    @Test
    fun parseNestedArrayOfArray() {
        val ast = parse(loadResource("collections.avdl"))
        val product = ast.modules.first().statements.first() as Type
        product.shape.value[3].reference shouldBe Reference.Iterable(
            reference = Reference.Iterable(
                reference = Reference.Primitive(
                    type = Reference.Primitive.Type.String(null),
                    isNullable = false,
                ),
                isNullable = false,
            ),
            isNullable = false,
        )
    }

    @Test
    fun parseMapOfArray() {
        val ast = parse(loadResource("collections.avdl"))
        val product = ast.modules.first().statements.first() as Type
        product.shape.value[4].reference shouldBe Reference.Dict(
            reference = Reference.Iterable(
                reference = Reference.Primitive(
                    type = Reference.Primitive.Type.String(null),
                    isNullable = false,
                ),
                isNullable = false,
            ),
            isNullable = false,
        )
    }

    @Test
    fun parseEnums() {
        val ast = parse(loadResource("enums.avdl"))
        val statements = ast.modules.flatMap { it.statements }.toList()
        val enums = statements.filterIsInstance<Enum>()
        enums.size shouldBe 2
        val color = enums.find { it.identifier.value == "Color" }!!
        color.entries shouldBe setOf("RED", "GREEN", "BLUE")
        val priority = enums.find { it.identifier.value == "Priority" }!!
        priority.entries shouldBe setOf("LOW", "MEDIUM", "HIGH")
    }

    @Test
    fun parseEnumReferenceFields() {
        val ast = parse(loadResource("enums.avdl"))
        val task = ast.modules.flatMap { it.statements }.toList().filterIsInstance<Type>()
            .find { it.identifier.value == "Task" }!!
        task.shape.value[1].reference shouldBe Reference.Custom("Color", isNullable = false)
        task.shape.value[2].reference shouldBe Reference.Custom("Priority", isNullable = false)
    }

    @Test
    fun parseExampleProtocol() {
        val ast = parse(loadResource("example.avdl"))
        val statements = ast.modules.flatMap { it.statements }.toList()
        val names = statements.map { it.identifier.value }
        names shouldContain "OAuthStatus"
        names shouldContain "ToDoStatus"
        names shouldContain "EmailAddress"
        names shouldContain "TwitterAccount"
        names shouldContain "ToDoItem"
        names shouldContain "User"
    }

    @Test
    fun parseExampleProtocolUserFields() {
        val ast = parse(loadResource("example.avdl"))
        val user = ast.modules.flatMap { it.statements }.toList()
            .filterIsInstance<Type>().find { it.identifier.value == "User" }!!
        user.shape.value.map { it.identifier.value } shouldContainExactly listOf(
            "id",
            "username",
            "passwordHash",
            "signupDate",
            "emailAddresses",
            "twitterAccounts",
            "toDoItems",
        )

        user.shape.value[4].reference shouldBe Reference.Iterable(
            reference = Reference.Custom("EmailAddress", isNullable = false),
            isNullable = false,
        )
    }

    @Test
    fun parseExampleProtocolEmailAddressNullableField() {
        val ast = parse(loadResource("example.avdl"))
        val email = ast.modules.flatMap { it.statements }.toList()
            .filterIsInstance<Type>().find { it.identifier.value == "EmailAddress" }!!
        val dateBounced = email.shape.value.find { it.identifier.value == "dateBounced" }!!
        dateBounced.reference shouldBe Reference.Primitive(
            type = Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P64, null),
            isNullable = true,
        )
    }

    @Test
    fun parseRecursiveRecord() {
        val source = """
            protocol Recursion {
                record Node {
                    string name;
                    array<Node> children;
                }
            }
        """.trimIndent()
        val ast = parse(source)
        val node = ast.modules.first().statements.first() as Type
        node.shape.value[1].reference shouldBe Reference.Iterable(
            reference = Reference.Custom("Node", isNullable = false),
            isNullable = false,
        )
    }

    @Test
    fun parseFieldWithDefaultValue() {
        val source = """
            protocol Defaults {
                record Settings {
                    boolean autosave = true;
                    int retries = 3;
                    string name = "anon";
                    array<string> tags = [];
                }
            }
        """.trimIndent()
        val ast = parse(source)
        val settings = ast.modules.first().statements.first() as Type
        settings.shape.value.size shouldBe 4
        settings.shape.value.map { it.identifier.value } shouldContainExactly listOf(
            "autosave",
            "retries",
            "name",
            "tags",
        )
    }

    @Test
    fun parseDocComments() {
        val source = """
            protocol DocTest {
                /** A user record */
                record User {
                    /** unique id */
                    string id;
                }
            }
        """.trimIndent()
        shouldNotThrow<Exception> { parse(source) }
    }

    @Test
    fun parseLineComments() {
        val source = """
            // top-level comment
            protocol Test {
                record A { // inline comment
                    string id;
                    // another comment
                    int value;
                }
            }
        """.trimIndent()
        val ast = parse(source)
        val a = ast.modules.first().statements.first() as Type
        a.shape.value.size shouldBe 2
    }

    @Test
    fun parseBlockComments() {
        val source = """
            /* block comment */
            protocol Test {
                /* another block */
                record A {
                    string id;
                }
            }
        """.trimIndent()
        shouldNotThrow<Exception> { parse(source) }
    }

    @Test
    fun parseAnnotationsOnFieldsAreIgnored() {
        val source = """
            protocol Test {
                record A {
                    @order("ascending") string id;
                    int value;
                }
            }
        """.trimIndent()
        val ast = parse(source)
        val a = ast.modules.first().statements.first() as Type
        a.shape.value.size shouldBe 2
    }

    @Test
    fun parseProtocolNamespaceAnnotation() {
        val source = """
            @namespace("com.example.test")
            protocol AnnotatedProtocol {
                record A {
                    string id;
                }
            }
        """.trimIndent()
        val protocol = AvroIdlParser.parseProtocol(source)
        protocol.namespace shouldBe "com.example.test"
        protocol.name shouldBe "AnnotatedProtocol"
    }

    @Test
    fun parseEmptyProtocolFails() {
        val source = """
            protocol Empty {
            }
        """.trimIndent()
        shouldThrow<Exception> {
            parse(source)
        }
    }

    @Test
    fun parseMissingProtocolKeywordFails() {
        val source = """
            record A {
                string id;
            }
        """.trimIndent()
        shouldThrow<Exception> {
            parse(source)
        }
    }

    @Test
    fun parseUnclosedBraceFails() {
        val source = """
            protocol Test {
                record A {
                    string id;
        """.trimIndent()
        shouldThrow<Exception> {
            parse(source)
        }
    }

    @Test
    fun parseInvalidPrimitiveTreatedAsCustom() {
        val source = """
            protocol Test {
                record A {
                    SomeUnknownType field;
                }
            }
        """.trimIndent()
        val ast = parse(source)
        val a = ast.modules.first().statements.first() as Type
        a.shape.value[0].reference shouldBe Reference.Custom("SomeUnknownType", isNullable = false)
    }

    @Test
    fun parseFixedTypeIsSkipped() {
        val source = """
            protocol Test {
                fixed Md5(16);
                record A {
                    string id;
                }
            }
        """.trimIndent()
        val ast = parse(source)
        val statements = ast.modules.flatMap { it.statements }.toList()
        statements.size shouldBe 1
        statements.first().identifier.value shouldBe "A"
    }

    @Test
    fun parseImportIsSkipped() {
        val source = """
            protocol Test {
                import schema "other.avsc";
                record A {
                    string id;
                }
            }
        """.trimIndent()
        val ast = parse(source)
        val statements = ast.modules.flatMap { it.statements }.toList()
        statements.size shouldBe 1
    }

    @Test
    fun parseCustomerProtocol() {
        val ast = parse(loadResource("customer.avdl"))
        val statements = ast.modules.flatMap { it.statements }.toList()
        val customer = statements.filterIsInstance<Type>().find { it.identifier.value == "Customer" }!!
        customer.shape.value.size shouldBe 7
        customer.shape.value.last().reference shouldBe Reference.Custom("Address", isNullable = false)
    }

    @Test
    fun parseUnionWithMultipleTypes() {
        val source = """
            protocol Test {
                record Wrapper {
                    union { null, string, int } value;
                }
            }
        """.trimIndent()
        // Wirespec converter rejects unions of multiple primitive types in a single field
        shouldThrow<Exception> { parse(source) }
            .message shouldBe "Cannot have multiple SimpleTypes in Union"
    }

    @Test
    fun parseMultipleProtocolsNotSupported() {
        val source = """
            protocol One {
                record A { string id; }
            }
            protocol Two {
                record B { string id; }
            }
        """.trimIndent()
        shouldThrow<Exception> { parse(source) }
    }

    @Test
    fun parseFieldWithoutSemicolonFails() {
        val source = """
            protocol Test {
                record A {
                    string id
                }
            }
        """.trimIndent()
        shouldThrow<Exception> { parse(source) }
    }

    @Test
    fun parseEnumWithSingleSymbol() {
        val source = """
            protocol Test {
                enum One {
                    SOLE
                }
                record A {
                    One value;
                }
            }
        """.trimIndent()
        val ast = parse(source)
        val statements = ast.modules.flatMap { it.statements }.toList()
        val enum = statements.filterIsInstance<Enum>().first()
        enum.entries shouldBe setOf("SOLE")
    }

    @Test
    fun parseEnumWithTrailingComma() {
        val source = """
            protocol Test {
                enum Trailing {
                    A, B, C,
                }
                record R {
                    Trailing value;
                }
            }
        """.trimIndent()
        val ast = parse(source)
        val enum = ast.modules.flatMap { it.statements }.toList().filterIsInstance<Enum>().first()
        enum.entries shouldBe setOf("A", "B", "C")
    }

    @Test
    fun parseFullExpectedTypeStructure() {
        val ast = parse(loadResource("simple.avdl"))
        val expected = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Pet"),
            extends = emptyList(),
            shape = Type.Shape(
                listOf(
                    Field(
                        identifier = FieldIdentifier("name"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = false,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("age"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P32, null),
                            isNullable = false,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("vaccinated"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Boolean,
                            isNullable = false,
                        ),
                    ),
                ),
            ),
        )
        assertEquals(expected, ast.modules.first().statements.first())
    }

    @Test
    fun parseHandlesWhitespaceAndNewlines() {
        val source = "protocol  T  {\n\nrecord    A   {\n  string\n  id\n  ;\n}\n}\n"
        shouldNotThrow<Exception> { parse(source) }
    }

    @Test
    fun parseHandlesCRLFLineEndings() {
        val source = "protocol T {\r\n  record A {\r\n    string id;\r\n  }\r\n}\r\n"
        val ast = parse(source)
        val a = ast.modules.first().statements.first() as Type
        a.shape.value.size shouldBe 1
    }

    @Test
    fun tokenizerHandlesEscapedStrings() {
        val tokens = AvroIdlTokenizer("\"hello\\nworld\"").tokenize()
        tokens.size shouldBe 1
        val literal = tokens.first() as AvroIdlToken.StringLiteral
        literal.value shouldBe "hello\nworld"
    }

    @Test
    fun tokenizerSkipsBlockComments() {
        val tokens = AvroIdlTokenizer("/* this is a block comment */ identifier").tokenize()
        tokens.size shouldBe 1
        (tokens.first() as AvroIdlToken.Identifier).value shouldBe "identifier"
    }

    @Test
    fun tokenizerSkipsLineComments() {
        val tokens = AvroIdlTokenizer("foo // bar\nbaz").tokenize()
        tokens.map { (it as AvroIdlToken.Identifier).value } shouldContainExactly listOf("foo", "baz")
    }

    @Test
    fun tokenizerCapturesDocComments() {
        val tokens = AvroIdlTokenizer("/** documentation */ id").tokenize()
        val doc = tokens.first() as AvroIdlToken.DocComment
        doc.value shouldBe "documentation"
    }

    @Test
    fun tokenizerCapturesMultilineDocComments() {
        val tokens = AvroIdlTokenizer(
            """
            /**
             * line one
             * line two
             */
            id
            """.trimIndent(),
        ).tokenize()
        val doc = tokens.first() as AvroIdlToken.DocComment
        assertTrue(doc.value.contains("line one"))
        assertTrue(doc.value.contains("line two"))
    }

    @Test
    fun tokenizerHandlesNumbers() {
        val tokens = AvroIdlTokenizer("42 -7 3.14 1e10").tokenize()
        tokens.size shouldBe 4
        tokens.forEach { it.shouldBeInstanceOf<AvroIdlToken.NumberLiteral>() }
    }

    @Test
    fun tokenizerHandlesAllSymbols() {
        val tokens = AvroIdlTokenizer("{ } ( ) [ ] < > , ; = @ ?").tokenize()
        tokens.size shouldBe 13
        tokens.forEach { it.shouldBeInstanceOf<AvroIdlToken.Symbol>() }
    }

    @Test
    fun parseFloatAndDoublePrimitives() {
        val source = """
            protocol Test {
                record A {
                    float a;
                    double b;
                }
            }
        """.trimIndent()
        val ast = parse(source)
        val a = ast.modules.first().statements.first() as Type
        a.shape.value[0].reference shouldBe Reference.Primitive(
            type = Reference.Primitive.Type.Number(Reference.Primitive.Type.Precision.P32, null),
            isNullable = false,
        )
        a.shape.value[1].reference shouldBe Reference.Primitive(
            type = Reference.Primitive.Type.Number(Reference.Primitive.Type.Precision.P64, null),
            isNullable = false,
        )
    }

    @Test
    fun parseIntAndLongPrimitives() {
        val source = """
            protocol Test {
                record A {
                    int a;
                    long b;
                }
            }
        """.trimIndent()
        val ast = parse(source)
        val a = ast.modules.first().statements.first() as Type
        a.shape.value[0].reference shouldBe Reference.Primitive(
            type = Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P32, null),
            isNullable = false,
        )
        a.shape.value[1].reference shouldBe Reference.Primitive(
            type = Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P64, null),
            isNullable = false,
        )
    }

    @Test
    fun parseRecordReferencingAnotherRecord() {
        val source = """
            protocol Test {
                record Inner {
                    string id;
                }
                record Outer {
                    Inner inner;
                }
            }
        """.trimIndent()
        val ast = parse(source)
        val statements = ast.modules.flatMap { it.statements }.toList()
        val outer = statements.filterIsInstance<Type>().find { it.identifier.value == "Outer" }!!
        outer.shape.value[0].reference shouldBe Reference.Custom("Inner", isNullable = false)
    }

    @Test
    fun parseErrorKeywordTreatedAsRecord() {
        val source = """
            protocol Test {
                error MyError {
                    string message;
                }
                record A {
                    MyError err;
                }
            }
        """.trimIndent()
        val ast = parse(source)
        val statements = ast.modules.flatMap { it.statements }.toList()
        statements.any { it.identifier.value == "MyError" } shouldBe true
    }
}
