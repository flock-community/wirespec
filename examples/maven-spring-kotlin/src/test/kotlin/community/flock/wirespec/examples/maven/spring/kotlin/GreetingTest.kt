package community.flock.wirespec.examples.maven.spring.kotlin

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class GreetingTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @Test
    fun `serves the Wirespec generated endpoint`() {
        // The handler is a Kotlin `suspend` function, so Spring MVC dispatches it
        // asynchronously; re-dispatch to obtain the completed response.
        val asyncResult = mockMvc.perform(get("/greeting"))
            .andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(asyncResult))
            .andExpect(status().isOk)
            .andExpect(content().json("""{"message":"Hello from Kotlin 2.0"}"""))
    }
}
