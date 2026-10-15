package community.flock.wirespec.ide.intellij.inlay

import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.emitters.java.JavaEmitter

class JavaMethodCodeVisionProviderTest : LightJavaCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor = JAVA_21

    fun testShowsMethodAndPathAboveImplementedHandlerMethod() {
        myFixture.addGeneratedSources(JavaEmitter(emitShared = EmitShared(true)))
        myFixture.configureByText(
            "TodoController.java",
            """
            import community.flock.wirespec.generated.endpoint.GetTodoById;
            import java.util.concurrent.CompletableFuture;

            public class TodoController implements GetTodoById.Handler {
                @Override
                public CompletableFuture<GetTodoById.Response<?>> getTodoById(GetTodoById.Request request) {
                    return null;
                }

                public void unrelated() {}
            }
            """.trimIndent(),
        )

        assertEquals(listOf("GET /todos/{id}"), JavaMethodCodeVisionProvider().texts(myFixture))
    }
}
