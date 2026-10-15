package community.flock.wirespec.ide.intellij.inlay

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.util.PathUtil
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import java.io.File

class KotlinMethodCodeVisionProviderTest : LightJavaCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinStdlibProjectDescriptor

    fun testShowsMethodAndPathAboveImplementedHandlerFunction() {
        myFixture.addGeneratedSources(KotlinEmitter(emitShared = EmitShared(true)))
        myFixture.configureByText(
            "TodoController.kt",
            """
            import community.flock.wirespec.generated.endpoint.GetTodoById

            class TodoController : GetTodoById.Handler {
                override suspend fun getTodoById(request: GetTodoById.Request): GetTodoById.Response<*> = TODO()

                fun unrelated() = Unit
            }
            """.trimIndent(),
        )

        assertEquals(listOf("GET /todos/{id}"), KotlinMethodCodeVisionProvider().texts(myFixture))
    }

    private object KotlinStdlibProjectDescriptor : DefaultLightProjectDescriptor() {
        override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
            super.configureModule(module, model, contentEntry)
            val stdlib = File(PathUtil.getJarPathForClass(KotlinVersion::class.java))
            PsiTestUtil.addLibrary(model, "kotlin-stdlib", stdlib.parent, stdlib.name)
        }
    }
}
