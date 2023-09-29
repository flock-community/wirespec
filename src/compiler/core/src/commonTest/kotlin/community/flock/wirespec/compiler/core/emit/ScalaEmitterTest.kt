package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.common.TestLogger
import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.tokenize.tokenize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ScalaEmitterTest {

    private val emitter = ScalaEmitter()

    @Test
    fun testShared() {
        println(emitter.shared)
    }
}
