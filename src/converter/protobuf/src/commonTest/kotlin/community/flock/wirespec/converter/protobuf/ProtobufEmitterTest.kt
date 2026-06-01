package community.flock.wirespec.converter.protobuf

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ProtobufEmitterTest {

    private fun parse(source: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("todo.ws"), source))).getOrNull() ?: error("Parsing failed.")

    private val logger = object : NoLogger {}.logger

    private fun emit(source: String): String = ProtobufEmitter(PackageName("community.flock.wirespec.generated.proto"))
        .emit(parse(source), logger).first().result.trim()

    @Test
    fun testRpcService() {
        val source =
            // language=ws
            """
            |type Todo { id: String, name: String, done: Boolean }
            |type TodoInput { name: String, done: Boolean }
            |rpc CreateTodo(todo: TodoInput) -> Todo
            |rpc DeleteTodo(id: String) -> Unit
            """.trimMargin()

        val expected =
            """
            |syntax = "proto3";
            |
            |package community.flock.wirespec.generated.proto;
            |
            |option java_multiple_files = true;
            |option java_package = "community.flock.wirespec.generated.proto";
            |
            |message Todo {
            |  string id = 1;
            |  string name = 2;
            |  bool done = 3;
            |}
            |
            |message TodoInput {
            |  string name = 1;
            |  bool done = 2;
            |}
            |
            |message CreateTodoRequest {
            |  TodoInput todo = 1;
            |}
            |
            |service CreateTodo {
            |  rpc CreateTodo (CreateTodoRequest) returns (Todo);
            |}
            |
            |message DeleteTodoRequest {
            |  string id = 1;
            |}
            |
            |message DeleteTodoResponse {}
            |
            |service DeleteTodo {
            |  rpc DeleteTodo (DeleteTodoRequest) returns (DeleteTodoResponse);
            |}
            """.trimMargin()

        emit(source) shouldBe expected
    }

    @Test
    fun testCollectionsAndNullable() {
        val source =
            // language=ws
            """
            |type Tag { label: String }
            |type Todo { id: String, name: String?, tags: Tag[], meta: { String } }
            |rpc ListTags(todo: Todo) -> Tag[]
            """.trimMargin()

        val proto = emit(source)

        proto shouldBe
            """
            |syntax = "proto3";
            |
            |package community.flock.wirespec.generated.proto;
            |
            |option java_multiple_files = true;
            |option java_package = "community.flock.wirespec.generated.proto";
            |
            |message Tag {
            |  string label = 1;
            |}
            |
            |message Todo {
            |  string id = 1;
            |  optional string name = 2;
            |  repeated Tag tags = 3;
            |  map<string, string> meta = 4;
            |}
            |
            |message ListTagsRequest {
            |  Todo todo = 1;
            |}
            |
            |message ListTagsResponse {
            |  repeated Tag value = 1;
            |}
            |
            |service ListTags {
            |  rpc ListTags (ListTagsRequest) returns (ListTagsResponse);
            |}
            """.trimMargin()
    }
}
