package community.flock.wirespec.compiler.core.emit.common

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class DefaultsTest {
    @Test
    fun testSpacer() {
        Spacer(0) shouldBe ""
        Spacer(1) shouldBe "  "
        Spacer(2) shouldBe "    "
    }
}
