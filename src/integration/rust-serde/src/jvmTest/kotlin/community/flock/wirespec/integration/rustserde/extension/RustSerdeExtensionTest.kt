package community.flock.wirespec.integration.rustserde.extension

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.extension.applyExtensions
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.rust.RustEmitter
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.test.Test

class RustSerdeExtensionTest {

    private val source =
        """
        |type ItemId = String(/^[a-z0-9-]+$/g)
        |enum Role { USER, ASSISTANT }
        |type UserMessage { role: Role, text: String }
        |type AssistantMessage { role: Role, text: String }
        |type Message = UserMessage | AssistantMessage
        |type Item { itemId: ItemId, `first-name`: String? }
        |type Request { item: Item }
        |endpoint PostItem POST Item /items/{id: ItemId} -> {
        |  200 -> Message
        |}
        """.trimMargin()

    private fun parse(): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri(""), source))).getOrNull() ?: error("Parsing failed.")

    private val emitted = RustEmitter(PackageName("generated"), EmitShared(true))
        .applyExtensions(nonEmptyListOf(RustSerdeExtension()))
        .emit(parse(), noLogger)

    private fun file(path: String): String = emitted.single { it.file == "generated/$path.rs" }.result

    @Test
    fun `A type derives serde and renames every field to its Wirespec name`() {
        file("model/item") shouldContain
            """
            |#[derive(Debug, Clone, Default, PartialEq)]
            |#[derive(serde::Serialize, serde::Deserialize)]
            |pub struct Item {
            |    #[serde(rename = "itemId")]
            |    pub item_id: ItemId,
            |    #[serde(rename = "first-name")]
            |    pub first_name: Option<String>,
            |}
            """.trimMargin()
    }

    @Test
    fun `A refined type is written as the value it wraps`() {
        file("model/item_id") shouldContain
            """
            |#[derive(serde::Serialize, serde::Deserialize)]
            |#[serde(transparent)]
            |pub struct ItemId {
            |    pub value: String,
            |}
            """.trimMargin()
    }

    @Test
    fun `An enum is written as its label`() {
        file("model/role").let {
            it shouldContain "impl serde::Serialize for Role {"
            it shouldContain "serializer.serialize_str(self.label())"
            it shouldContain "impl<'de> serde::Deserialize<'de> for Role {"
        }
    }

    @Test
    fun `A union names its member in a type property`() {
        file("model/message") shouldContain
            """
            |#[derive(serde::Serialize, serde::Deserialize)]
            |#[serde(tag = "type")]
            |#[derive(Debug, Clone, PartialEq)]
            |pub enum Message {
            """.trimMargin()
    }

    @Test
    fun `The structs of an endpoint are left as they are, also when a model shares their name`() {
        file("model/request") shouldContain "#[derive(serde::Serialize, serde::Deserialize)]"
        file("endpoint/post_item") shouldNotContain "serde"
    }

    @Test
    fun `The body serializers of the shared module take a serde body`() {
        file("wirespec").let {
            it shouldContain "fn serialize_body<T: serde::Serialize + 'static>(&self, t: &T, r#type: TypeId) -> Vec<u8>;"
            it shouldContain "fn deserialize_body<T: serde::de::DeserializeOwned + 'static>(&self, raw: &[u8], r#type: TypeId) -> T;"
        }
    }

    @Test
    fun `Without the extension nothing refers to serde`() {
        RustEmitter(PackageName("generated"), EmitShared(true)).emit(parse(), noLogger)
            .forEach { it.result shouldNotContain "serde" }
    }
}
