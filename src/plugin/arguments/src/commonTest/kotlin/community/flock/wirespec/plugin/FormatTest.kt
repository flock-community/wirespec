package community.flock.wirespec.plugin

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class FormatTest {
    @Test
    fun testFormat() {
        Format.toString() shouldBe "OpenAPIV2, OpenAPIV3, Avro"
    }
}
