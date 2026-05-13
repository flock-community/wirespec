package community.flock.wirespec.verify

import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import io.kotest.core.spec.style.FunSpec

class VerifyFullEndpointTest : FunSpec({

    languages.values.forEach { lang ->
        test("full endpoint - $lang") {
            lang.start(
                name = "full-endpoint",
                fixture = CompileFullEndpointTest,
            )
            lang.compile()
        }
    }
})
