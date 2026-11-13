package community.flock.wirespec.plugin

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.SpecInfo
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.java.JavaEmitter
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class SpecInfoTest {

    private val ast = object : ParseContext, NoLogger {}
        .parse(nonEmptyListOf(ModuleContent(FileUri("api.ws"), "type Customer { id: String }")))
        .shouldBeRight()

    private val info = SpecInfo(title = "Customer API", version = "2.4.0")

    @Test
    fun theLanguagesWithADocumentGetTheTitleAndVersionOfTheBuild() {
        listOf(Language.OpenAPIV2, Language.OpenAPIV3, Language.AsyncAPI).forEach { language ->
            language.toEmitter(PackageName("generated"), EmitShared(false), info)
                .emit(ast, noLogger)
                .single()
                .result
                .apply {
                    shouldContain("\"title\": \"Customer API\"")
                    shouldContain("\"version\": \"2.4.0\"")
                }
        }
    }

    @Test
    fun theOtherLanguagesHaveNoTitleOrVersionToSet() {
        Language.Java.toEmitter(PackageName("generated"), EmitShared(false), info).shouldBeInstanceOf<JavaEmitter>()
    }
}
