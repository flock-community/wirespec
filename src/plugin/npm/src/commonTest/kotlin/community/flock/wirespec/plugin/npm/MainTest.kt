package community.flock.wirespec.plugin.npm

import com.goncalossilva.resources.Resource
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.lib.produce
import community.flock.wirespec.compiler.utils.noLogger
import kotlin.test.Test
import kotlin.test.assertTrue

class MainTest {

    @Test
    fun testEmit() {
        val wirespecFile = Resource("src/commonTest/resources/person.ws").readText()
        println(wirespecFile)
        val res = WirespecSpec.parse(wirespecFile)(noLogger).getOrNull()
        println(res)
        if(res != null){
            val test = emit(res.produce(), Emitters.OPENAPI_V2, "")
            assertTrue { test.first().result.startsWith("""{"swagger":"2.0"""") }
        }


    }
}