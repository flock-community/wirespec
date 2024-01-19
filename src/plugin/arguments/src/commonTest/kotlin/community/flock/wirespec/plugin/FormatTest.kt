package community.flock.wirespec.plugin

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class FormatTest {
    @Test
    fun testFormat() {
        Format.toString() shouldBe "OpenApiV2, OpenApiV3, Avro"
    }
}
