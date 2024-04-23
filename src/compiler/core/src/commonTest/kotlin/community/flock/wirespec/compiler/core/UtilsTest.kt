package community.flock.wirespec.compiler.core

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class UtilsTest {

    @Test
    fun testHasBackticks() {
        "`type`".hasBackticks() shouldBe true
        "`type".hasBackticks() shouldBe false
        "type`".hasBackticks() shouldBe false
        "type".hasBackticks() shouldBe false
        "`".hasBackticks() shouldBe false
    }

    @Test
    fun testRemoveBackticks() {
        "`type`".removeBackticks() shouldBe "type"
        "```".removeBackticks() shouldBe "`"
        "`".removeBackticks() shouldBe "`"
    }

    @Test
    fun testAddBackticks() {
        "type".addBackticks() shouldBe "`type`"
    }

}
