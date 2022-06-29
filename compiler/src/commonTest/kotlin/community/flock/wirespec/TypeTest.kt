package community.flock.wirespec

import community.flock.wirespec.compiler.WireSpec
import community.flock.wirespec.compiler.compile
import community.flock.wirespec.compiler.emit.KotlinEmitter
import community.flock.wirespec.io.WireSpecFile
import platform.posix.opendir
import kotlin.test.Ignore
import kotlin.test.Test

class TypeTest {

    @Ignore
    fun testTypes() {
        WireSpecFile("compiler/src/commonTest/resources/type").read()
            .let(WireSpec::compile)(KotlinEmitter)
            .run { isNotEmpty() }
            .let(::assert)
    }
}
