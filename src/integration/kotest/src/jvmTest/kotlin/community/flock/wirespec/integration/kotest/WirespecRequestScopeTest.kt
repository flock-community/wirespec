package community.flock.wirespec.integration.kotest

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class WirespecRequestScopeTest {
    @Test
    fun requestScopeReflectsCurrentBlockAndRestoresOnExit() {
        runBlocking {
            val scope = WirespecRequestScope<String>()

            scope.current() shouldBe null

            scope.with("outer") {
                scope.current() shouldBe "outer"
                scope.with("inner") {
                    scope.current() shouldBe "inner"
                }
                scope.current() shouldBe "outer"
            }

            scope.current() shouldBe null
        }
    }
}
