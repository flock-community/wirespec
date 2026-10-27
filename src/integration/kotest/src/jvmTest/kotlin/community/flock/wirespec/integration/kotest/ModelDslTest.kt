package community.flock.wirespec.integration.kotest

import community.flock.wirespec.integration.kotest.dsl.draw
import community.flock.wirespec.integration.kotest.kotlin.generated.channel.SessionEvents
import community.flock.wirespec.integration.kotest.kotlin.generated.endpoint.CreateSession
import community.flock.wirespec.integration.kotest.kotlin.generated.endpoint.ImportSessions
import community.flock.wirespec.integration.kotest.kotlin.generated.kotest.generate
import community.flock.wirespec.integration.kotest.kotlin.generated.model.Node
import community.flock.wirespec.integration.kotest.kotlin.generated.model.Session
import community.flock.wirespec.integration.kotest.kotlin.generated.model.Tag
import community.flock.wirespec.integration.kotest.kotlin.generated.model.Token
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.RandomSource
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class ModelDslTest {

    private val rs = RandomSource.seeded(42L)

    @Test
    fun aSingleFieldRecordIsPinnedAsAWholeValue() {
        Session.generate { token(Token("pinned")) }.draw(rs).token shouldBe Token("pinned")
    }

    @Test
    fun refinedFieldsArePinnedByTheirInnerValueInEveryShape() {
        val session = Session.generate {
            tag("abc")
            tags(listOf("abc", "def"))
            tagsByKey(mapOf("key" to "abc"))
        }.draw(rs)

        session.tag shouldBe Tag("abc")
        session.tags shouldBe listOf(Tag("abc"), Tag("def"))
        session.tagsByKey shouldBe mapOf("key" to Tag("abc"))
        Session.generate { tag(null) }.draw(rs).tag.shouldBeNull()
    }

    @Test
    fun aNullableRecordBlockPinsTheRecordWhenOneIsGenerated() {
        val previous = (1L..20L).mapNotNull { seed -> Session.generate { previousBlock { iss("pinned") } }.draw(RandomSource.seeded(seed)).previous }

        previous.shouldNotBeEmpty()
        previous.forEach { it.iss shouldBe "pinned" }
    }

    @Test
    fun recursiveBlocksReachEveryDepth() {
        val node = Node.generate { childrenBlock { childrenBlock { name("leaf") } } }.draw(rs)

        node.children.flatMap { it.children }.shouldNotBeEmpty().forEach { it.name shouldBe "leaf" }
    }

    @Test
    fun onlyThePinnedFieldDiffersFromTheGeneratedRecord() {
        val generated = Session.generate().draw(RandomSource.seeded(7L))
        val pinned = Session.generate { tag("abc") }.draw(RandomSource.seeded(7L))

        pinned shouldBe generated.copy(tag = Tag("abc"))
    }

    @Test
    fun endpointBodiesAndChannelMessagesShareTheRecordBuilder() = runBlocking<Unit> {
        CreateSession.generate.request { body { tag("abc") } }.draw(rs).body.tag shouldBe Tag("abc")

        val imported = ImportSessions.generate.request {
            body { tokenBlock { iss("pinned") } }
            bodyCount = 2..2
        }.draw(rs).body
        imported shouldHaveSize 2
        imported.forEach { it.token shouldBe Token("pinned") }

        SessionEvents.generate.message { tag("abc") }.draw(rs).tag shouldBe Tag("abc")
    }
}
