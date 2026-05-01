package community.flock.wirespec.converter.avro

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import kotlin.test.Test

class AvroIdlEmitterTest {

    private fun parse(source: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("test.ws"), source))).getOrNull() ?: error("Parsing failed.")

    @Test
    fun emitsProtocolWrapper() {
        val ast = parse("type Pet { name: String }")
        val output = AvroIdlEmitter.emit(ast.modules.first(), "PetProtocol")
        output shouldStartWith "protocol PetProtocol {\n"
        output.trim().endsWith("}") shouldBe true
    }

    @Test
    fun emitsSimpleRecord() {
        val ast = parse("type Pet { name: String, age: Integer }")
        val output = AvroIdlEmitter.emit(ast.modules.first(), "Test")
        output shouldContain "record Pet {"
        output shouldContain "string name;"
        output shouldContain "long age;"
    }

    @Test
    fun emitsAllPrimitives() {
        val ast = parse(
            """
            type AllPrimitives {
                s: String,
                i: Integer,
                n: Number,
                b: Boolean
            }
            """.trimIndent(),
        )
        val output = AvroIdlEmitter.emit(ast.modules.first(), "Test")
        output shouldContain "string s;"
        output shouldContain "long i;"
        output shouldContain "double n;"
        output shouldContain "boolean b;"
    }

    @Test
    fun emitsNullableFieldAsUnionWithNull() {
        val ast = parse("type Pet { name: String, nickname: String? }")
        val output = AvroIdlEmitter.emit(ast.modules.first(), "Test")
        output shouldContain "union { null, string } nickname;"
        output shouldContain "string name;"
    }

    @Test
    fun emitsArrayField() {
        val ast = parse("type Pet { tags: String[] }")
        val output = AvroIdlEmitter.emit(ast.modules.first(), "Test")
        output shouldContain "array<string> tags;"
    }

    @Test
    fun emitsNullableArrayField() {
        val ast = parse("type Pet { tags: String[]? }")
        val output = AvroIdlEmitter.emit(ast.modules.first(), "Test")
        output shouldContain "union { null, array<string> } tags;"
    }

    @Test
    fun emitsCustomTypeReference() {
        val ast = parse(
            """
            type Address { street: String }
            type Pet { home: Address }
            """.trimIndent(),
        )
        val output = AvroIdlEmitter.emit(ast.modules.first(), "Test")
        output shouldContain "record Address {"
        output shouldContain "Address home;"
    }

    @Test
    fun emitsNullableCustomReference() {
        val ast = parse(
            """
            type Address { street: String }
            type Pet { home: Address? }
            """.trimIndent(),
        )
        val output = AvroIdlEmitter.emit(ast.modules.first(), "Test")
        output shouldContain "union { null, Address } home;"
    }

    @Test
    fun emitsEnum() {
        val ast = parse(
            """
            enum Status { PUBLIC, PRIVATE }
            type Doc { status: Status }
            """.trimIndent(),
        )
        val output = AvroIdlEmitter.emit(ast.modules.first(), "Test")
        output shouldContain "enum Status {"
        output shouldContain "PUBLIC, PRIVATE"
    }

    @Test
    fun emitsUnionAsWrapperRecord() {
        val ast = parse(
            """
            type Left { left: String }
            type Right { right: String }
            type Either = Left | Right
            type Doc { value: Either }
            """.trimIndent(),
        )
        val output = AvroIdlEmitter.emit(ast.modules.first(), "Test")
        output shouldContain "record Either {"
        output shouldContain "union {"
        output shouldContain "Left"
        output shouldContain "Right"
    }

    @Test
    fun emitTodoWs() {
        val source = """
            enum Status {
                PUBLIC,
                PRIVATE
            }

            type Todo {
                id: String,
                name: String?,
                done: Boolean,
                tags: String[],
                status: Status
            }
        """.trimIndent()
        val ast = parse(source)
        val output = AvroIdlEmitter.emit(ast.modules.first(), "TodoProtocol")
        output shouldStartWith "protocol TodoProtocol {\n"
        output shouldContain "enum Status {"
        output shouldContain "record Todo {"
        output shouldContain "string id;"
        output shouldContain "union { null, string } name;"
        output shouldContain "boolean done;"
        output shouldContain "array<string> tags;"
        output shouldContain "Status status;"
    }

    @Test
    fun emitFromCompileMinimalEndpointTest() {
        val result = CompileMinimalEndpointTest.compiler {
            AvroIdlEmitter
        }
        val output = result.shouldBeRight()
        output shouldContain "protocol"
        output shouldContain "record TodoDto {"
        output shouldContain "string description;"
    }

    @Test
    fun emitFromCompileFullEndpointTest() {
        val result = CompileFullEndpointTest.compiler {
            AvroIdlEmitter
        }
        val output = result.shouldBeRight()
        output shouldContain "record PotentialTodoDto {"
        output shouldContain "record Token {"
        output shouldContain "record TodoDto {"
        output shouldContain "record Error {"
        output shouldContain "string name;"
        output shouldContain "boolean done;"
        output shouldContain "long code;"
    }

    @Test
    fun emitFromCompileEnumTest() {
        val result = CompileEnumTest.compiler {
            AvroIdlEmitter
        }
        val output = result.shouldBeRight()
        output shouldContain "enum MyAwesomeEnum {"
        output shouldContain "ONE"
        output shouldContain "Two"
        output shouldContain "THREE_MORE"
    }

    @Test
    fun emitFromCompileChannelTestProducesEmptyProtocol() {
        val result = CompileChannelTest.compiler {
            AvroIdlEmitter
        }
        val output = result.shouldBeRight()
        output shouldStartWith "protocol"
        output shouldNotContain "record"
        output shouldNotContain "enum"
    }

    @Test
    fun emitFromCompileRefinedTestRendersRefinedAsComment() {
        val result = CompileRefinedTest.compiler {
            AvroIdlEmitter
        }
        val output = result.shouldBeRight()
        output shouldContain "// refined"
    }

    @Test
    fun emitFromCompileUnionTest() {
        val result = CompileUnionTest.compiler {
            AvroIdlEmitter
        }
        val output = result.shouldBeRight()
        output shouldContain "record UserAccount {"
        output shouldContain "UserAccountPassword"
        output shouldContain "UserAccountToken"
        output shouldContain "record User {"
    }

    @Test
    fun emitFromCompileTypeTest() {
        val result = CompileTypeTest.compiler { AvroIdlEmitter }
        val output = result.shouldBeRight()
        output shouldContain "record Request {"
        output shouldContain "array<string> params;"
        output shouldContain "map<string> headers;"
        output shouldContain "union { null, string } BODY_TYPE;"
        output shouldContain "union { null, map<array<string>> } body;"
    }

    @Test
    fun emitWithDictField() {
        val ast = parse("type Mappy { entries: { String } }")
        val output = AvroIdlEmitter.emit(ast.modules.first(), "Test")
        output shouldContain "map<string> entries;"
    }

    @Test
    fun emitWithDictOfArrayField() {
        val ast = parse("type Outer { values: { String[] } }")
        val output = AvroIdlEmitter.emit(ast.modules.first(), "Test")
        output shouldContain "map<array<string>> values;"
    }

    @Test
    fun emitWithIntegerPrecisionLong() {
        val ast = parse("type X { id: Integer }")
        val output = AvroIdlEmitter.emit(ast.modules.first(), "Test")
        output shouldContain "long id;"
    }

    @Test
    fun roundTripParseEmitParseProducesEqualStructure() {
        val source = """
            protocol Test {
                record User {
                    string id;
                    union { null, string } nickname;
                    int age;
                    array<string> tags;
                    map<int> ratings;
                }
            }
        """.trimIndent()

        val ast1 = AvroIdlParser.parse(ModuleContent(FileUri("test.ws"), source), true)
        val emitted = AvroIdlEmitter.emit(ast1.modules.first(), "Test")
        val ast2 = AvroIdlParser.parse(ModuleContent(FileUri("test.ws"), emitted), true)

        ast1.modules.first().statements.toList().map { it.identifier.value } shouldBe
            ast2.modules.first().statements.toList().map { it.identifier.value }
    }

    @Test
    fun emitterUsesAvroIdlExtension() {
        AvroIdlEmitter.extension shouldBe community.flock.wirespec.compiler.core.emit.FileExtension.AvroIdl
        AvroIdlEmitter.extension.value shouldBe "avdl"
    }

    @Test
    fun emitDerivesProtocolNameFromFileUri() {
        val ast = parse("type Pet { name: String }")
        val moduleWithUri = community.flock.wirespec.compiler.core.parse.ast.Module(
            fileUri = FileUri("path/to/myfile.ws"),
            statements = ast.modules.first().statements,
        )
        val output = AvroIdlEmitter.emit(
            community.flock.wirespec.compiler.core.parse.ast.Root(nonEmptyListOf(moduleWithUri)),
            noLogger,
        )
        output.first().result shouldContain "protocol MyfileProtocol {"
    }
}
