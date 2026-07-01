package community.flock.wirespec.integration.kotest.context

import community.flock.wirespec.integration.kotest.WirespecRequestScope
import community.flock.wirespec.integration.kotest.WirespecTestContext
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KType
import kotlin.test.Test

class WirespecContextRegistryTest {
    private val noopSerialization =
        object : Wirespec.Serialization {
            override fun <T : Any> serializeBody(t: T, type: KType): ByteArray = error("unused")

            override fun <T : Any> deserializeBody(raw: ByteArray, type: KType): T = error("unused")

            override fun <T : Any> serializePath(t: T, type: KType): String = error("unused")

            override fun <T : Any> deserializePath(raw: String, type: KType): T = error("unused")

            override fun <T : Any> serializeParam(value: T, type: KType): List<String> = error("unused")

            override fun <T : Any> deserializeParam(values: List<String>, type: KType): T = error("unused")
        }

    private fun context() = WirespecTestContext(
        transportation = Wirespec.Transportation { error("unused") },
        serialization = noopSerialization,
    )

    @Test
    fun providerReturnsRegisteredContextAndNullAfterUnregister() {
        val provider = RegistryContextProvider()
        val spec = object : FunSpec() {}
        val ctx = context()

        provider.endpointContext(spec) shouldBe null

        WirespecContextRegistry.registerEndpoint(spec, ctx)
        provider.endpointContext(spec) shouldBe ctx

        WirespecContextRegistry.unregister(spec)
        provider.endpointContext(spec) shouldBe null
    }

    @Test
    fun contextsAreIsolatedPerSpecInstance() {
        val provider = RegistryContextProvider()
        val specA = object : FunSpec() {}
        val specB = object : FunSpec() {}
        val ctxA = context()

        WirespecContextRegistry.registerEndpoint(specA, ctxA)

        provider.endpointContext(specA) shouldBe ctxA
        provider.endpointContext(specB) shouldBe null

        WirespecContextRegistry.unregister(specA)
    }

    @Test
    fun providerIsDiscoveredViaServiceLoader() {
        val discovered = ContextRegistry.providers.any { it is RegistryContextProvider }
        discovered shouldBe true
    }

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
