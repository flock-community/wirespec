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

    @Test
    fun testConcatGenerics() {
        "List<String>".concatGenerics() shouldBe "ListString"
        "Map<String, List<String>>".concatGenerics() shouldBe "MapStringListString"
        "java.util.List<String>".concatGenerics() shouldBe "ListString"
    }

    @Test
    fun testRemoveCommentMarkers() {
        "/* Simple comment */".removeCommentMarkers() shouldBe "Simple comment"
        "/*    Padded    */".removeCommentMarkers() shouldBe "Padded"
        "/**/".removeCommentMarkers() shouldBe ""

        """/* Multiple
           | Lines
           | Here */
        """.trimMargin().removeCommentMarkers() shouldBe """Multiple
           | Lines
           | Here
        """.trimMargin()
    }
}
