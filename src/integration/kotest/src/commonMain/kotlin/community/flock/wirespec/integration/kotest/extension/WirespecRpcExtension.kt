package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.integration.kotest.runtime.WirespecSeed
import community.flock.wirespec.integration.kotest.runtime.orNew
import io.kotest.core.extensions.TestCaseExtension
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.reflect.KClass

public class WirespecRpcExtension(private val rpc: WirespecRpcContext) : TestCaseExtension {

    public constructor(vararg services: Any) : this(WirespecRpcContext(*services))

    override suspend fun intercept(
        testCase: TestCase,
        execute: suspend (TestCase) -> TestResult,
    ): TestResult {
        val seed = coroutineContext[WirespecSeed].orNew()
        return withContext(rpc + seed) { execute(testCase) }
    }
}

public class WirespecRpcContext(vararg services: Any) : AbstractCoroutineContextElement(Key) {

    public val services: List<Any> = services.toList()

    @Suppress("UNCHECKED_CAST")
    internal fun <S : Any> service(serviceClass: KClass<S>): S = services.filter(serviceClass::isInstance).let { matching ->
        when (matching.size) {
            1 -> matching.single() as S
            0 -> error("No ${serviceClass.qualifiedName} registered. Pass an implementation to `WirespecRpcExtension(...)` on the spec.")
            else -> error("${matching.size} registered services implement ${serviceClass.qualifiedName}; register exactly one.")
        }
    }

    public companion object Key : CoroutineContext.Key<WirespecRpcContext>
}

internal suspend fun currentRpcContext(): WirespecRpcContext = coroutineContext[WirespecRpcContext] ?: error(
    "No WirespecRpcContext configured. Register " +
        "`WirespecRpcExtension(services)` on the spec.",
)
