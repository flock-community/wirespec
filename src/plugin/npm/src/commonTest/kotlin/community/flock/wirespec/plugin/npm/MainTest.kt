package community.flock.wirespec.plugin.npm

import WsMatchResult
import com.goncalossilva.resources.Resource
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.lib.WsEndpoint
import community.flock.wirespec.compiler.lib.WsLiteral
import community.flock.wirespec.compiler.lib.WsMethod
import community.flock.wirespec.compiler.lib.produce
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.collections.contentEquals

class MainTest {

    @Test
    fun testEmit() {
        val file = Resource("src/commonTest/resources/person.ws").readText()
        val res = WirespecSpec.parse(file)(noLogger).getOrNull()
        assertNotNull(res)
        val openApiV2 = emit(res.produce(), Emitters.OPENAPI_V2, "")
        val openApiV3 = emit(res.produce(), Emitters.OPENAPI_V3, "")
        assertEquals("""{"swagger":"2.0"""", openApiV2.first().result.substring(0, 16))
        assertEquals("""{"openapi":"3.0.0"""", openApiV3.first().result.substring(0, 18))
    }

    @Test
    fun testRouter() {
        val file = Resource("src/commonTest/resources/person.ws").readText()
        val ast = WirespecSpec.parse(file)(noLogger).getOrNull()
        assertNotNull(ast)

        val router = router(ast.produce())
        val res = router.match("GET", "/todos")
        val expected = WsMatchResult(
            endpoint = ast
                .filterIsInstance<Endpoint>()
                .find { it.identifier.value == "GetTodos" }
                ?.produce()
                ?: error("Not found"),
            params = mapOf(),
            query = mapOf(),
        )
        res?.endpoint?.requests shouldBe expected.endpoint.requests
        res?.endpoint?.responses?.shouldHaveSize(1)
        res?.endpoint?.responses?.get(0)?.status shouldBe expected.endpoint.responses[0].status
        res?.endpoint?.responses?.get(0)?.content shouldBe expected.endpoint.responses[0].content
        res?.endpoint?.responses?.get(0)?.headers contentEquals expected.endpoint.responses[0].headers
        res?.endpoint?.requests?.shouldHaveSize(1)
        res?.endpoint?.requests?.get(0) shouldBe expected.endpoint.requests[0]
    }
}