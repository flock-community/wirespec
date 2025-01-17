package community.flock.wirespec.plugin.npm

import com.goncalossilva.resources.Resource
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.lib.produce
import community.flock.wirespec.compiler.utils.noLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MainTest {

    @Test
    fun testEmit() {
        val file = Resource("src/commonTest/resources/person.ws").readText()
        val res = object : ParseContext {
            override val spec = WirespecSpec
            override val logger = noLogger
        }.parse(file).getOrNull()
        assertNotNull(res)
        val openApiV2 = emit(res.produce(), Emitters.OPENAPI_V2, "")
        val openApiV3 = emit(res.produce(), Emitters.OPENAPI_V3, "")
        assertEquals("""{"swagger":"2.0"""", openApiV2.first().result.substring(0, 16))
        assertEquals("""{"openapi":"3.0.0"""", openApiV3.first().result.substring(0, 18))
    }
}
