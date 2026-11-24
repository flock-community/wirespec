package community.flock.wirespec.integration.jsonrpc.extension

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.emit.IrEmitter
import community.flock.wirespec.compiler.core.ir.extension.SyncExtension
import community.flock.wirespec.compiler.core.ir.extension.applyExtensions
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.emitters.python.PythonEmitter
import community.flock.wirespec.emitters.rust.RustEmitter
import community.flock.wirespec.emitters.scala.ScalaEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptEmitter
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.test.Test

class JsonRpcExtensionTest {

    private val source =
        """
        |type User { name: String }
        |type UserNotFound { id: String }
        |rpc GetUser { id: String } -> User ! UserNotFound
        |@JsonRpcMethod("users/find")
        |rpc FindUsers { name: String? } -> User[]
        """.trimMargin()

    private fun parse(source: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri(""), source))).getOrNull() ?: error("Parsing failed.")

    private val packageName = PackageName("generated")

    private fun IrEmitter.emitWithJsonRpc(source: String = this@JsonRpcExtensionTest.source): List<Emitted> = applyExtensions(nonEmptyListOf(JsonRpcExtension(packageName, extension)))
        .emit(parse(source), noLogger)

    private fun List<Emitted>.file(path: String): String = single { it.file == path }.result

    @Test
    fun `Kotlin gets a client that implements every Service, and a server that takes them all`() {
        KotlinEmitter(packageName).emitWithJsonRpc().file("generated/JsonRpc.kt").let {
            it shouldContain ") : GetUser.Service, FindUsers.Service {"
            it shouldContain "override suspend fun getUser(id: String): GetUser.Response = JsonRpc.call<GetUser.Response>("
            it shouldContain "method = \"users/find\","
            it shouldContain "name?.let { \"name\" to JsonRpc.encode(it) },"
            it shouldContain ") where S : GetUser.Service, S : FindUsers.Service {"
            it shouldContain "public suspend fun handle(request: Wirespec.RawRequest): Wirespec.RawResponse"
        }
    }

    @Test
    fun `With sync Services the Kotlin client does not implement them, and the server does not suspend`() {
        KotlinEmitter(packageName)
            .applyExtensions(nonEmptyListOf(SyncExtension(FileExtension.Kotlin), JsonRpcExtension(packageName, FileExtension.Kotlin)))
            .emit(parse(source), noLogger)
            .file("generated/JsonRpc.kt")
            .let {
                it shouldNotContain ") : GetUser.Service"
                it shouldContain "public suspend fun getUser(id: String): GetUser.Response"
                it shouldContain "public fun handle(request: Wirespec.RawRequest): Wirespec.RawResponse"
            }
    }

    @Test
    fun `Java gets a file per class`() {
        JavaEmitter(packageName).emitWithJsonRpc().map { it.file }
            .shouldContainAll("generated/JsonRpcClient.java", "generated/JsonRpcServer.java", "generated/JsonRpcException.java", "generated/JsonRpc.java")
        JavaEmitter(packageName).emitWithJsonRpc().file("generated/JsonRpcServer.java") shouldContain
            "public final class JsonRpcServer<S extends GetUser.Service & FindUsers.Service> {"
    }

    @Test
    fun `Scala gets a client that implements every Service`() {
        ScalaEmitter(packageName).emitWithJsonRpc().file("generated/JsonRpc.scala").let {
            it shouldContain "extends GetUser.Service with FindUsers.Service {"
            it shouldContain "class JsonRpcServer[S <: GetUser.Service with FindUsers.Service]"
            it shouldContain "private[generated] def call[T]"
        }
    }

    @Test
    fun `TypeScript marks the error of an rpc, because its Result and Error look alike`() {
        TypeScriptEmitter().emitWithJsonRpc().file("JsonRpc.ts").let {
            it shouldContain "import {GetUser} from './rpc/GetUser'"
            it shouldContain "(json) => JsonRpc.error(this.serialization.deserializeBody<UserNotFound>(bytes(json), \"UserNotFound\")),"
            it shouldContain "return JsonRpc.isError(response) ? failure(JsonRpc.SERVER_ERROR"
        }
    }

    @Test
    fun `Python reads the envelope with the json module`() {
        PythonEmitter(packageName, EmitShared(true)).emitWithJsonRpc().file("generated/JsonRpc.py").let {
            it shouldContain "import json as _json"
            it shouldContain "class JsonRpcClient(GetUser.Service, FindUsers.Service):"
            it shouldContain "async def GetUser(self, id: str) -> GetUser.Response:"
            it shouldContain "if isinstance(response, GetUser.Error):"
        }
    }

    @Test
    fun `Rust gets a module of its own, whose client returns a Result`() {
        RustEmitter(packageName, EmitShared(true)).emitWithJsonRpc().let { emitted ->
            emitted.file("generated/mod.rs") shouldContain "pub mod json_rpc;"
            emitted.file("generated/json_rpc.rs").let {
                it shouldContain "use super::rpc::get_user::GetUser;"
                it shouldContain "pub async fn get_user(&self, id: &str) -> Result<GetUser::Response, JsonRpcError> {"
                it shouldContain "Reply::Error { code: SERVER_ERROR, data: Some(json), .. } =>"
                it shouldContain "impl<S: Serialization, Svc: GetUser::Service + FindUsers::Service> JsonRpcServer<S, Svc> {"
                it shouldContain "let response = GetUser::Service::get_user(&self.service, &id).await;"
            }
        }
    }

    @Test
    fun `A contract without rpcs is left as it is`() {
        val endpoints = "type User { name: String }\nendpoint GetUsers GET /users -> { 200 -> User[] }"
        KotlinEmitter(packageName).emitWithJsonRpc(endpoints) shouldBe KotlinEmitter(packageName).emit(parse(endpoints), noLogger)
    }
}
