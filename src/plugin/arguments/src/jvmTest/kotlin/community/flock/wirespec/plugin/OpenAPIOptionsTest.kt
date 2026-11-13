package community.flock.wirespec.plugin

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class OpenAPIOptionsTest {

    @Test
    fun theOpenAPIEmittersGetTheTitleAndVersionOfTheBuild() {
        val ast = object : ParseContext, NoLogger {}
            .parse(nonEmptyListOf(ModuleContent(FileUri("api.ws"), "type Customer { id: String }")))
            .shouldBeRight()

        listOf(Language.OpenAPIV2, Language.OpenAPIV3).forEach { language ->
            language.toEmitter(PackageName("generated"), EmitShared(false), openApiTitle = "Customer API", openApiVersion = "2.4.0")
                .emit(ast, noLogger)
                .single()
                .result
                .apply {
                    shouldContain("\"title\": \"Customer API\"")
                    shouldContain("\"version\": \"2.4.0\"")
                }
        }
    }
}
