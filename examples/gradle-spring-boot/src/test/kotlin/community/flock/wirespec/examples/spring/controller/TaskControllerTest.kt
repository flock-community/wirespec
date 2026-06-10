package community.flock.wirespec.examples.spring.controller

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.examples.spring.generated.model.Member
import community.flock.wirespec.examples.spring.generated.model.MemberId
import community.flock.wirespec.examples.spring.repository.MemberRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TaskControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var memberRepository: MemberRepository

    private val ownerId = "11111111-1111-1111-1111-111111111111"

    @BeforeEach
    fun seedOwner() = runBlocking {
        memberRepository.save(
            Member(
                id = MemberId(ownerId),
                ref = "alice",
                name = "Alice Anderson",
                email = "alice@example.com",
            ),
        )
        Unit
    }

    private fun MockMvc.dispatch(builder: RequestBuilder): MvcResult {
        val started = perform(builder).andExpect(request().asyncStarted()).andReturn()
        return perform(asyncDispatch(started)).andReturn()
    }

    private fun seedProject(): String {
        val result = mockMvc.dispatch(
            post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("ref" to "apollo", "name" to "Apollo", "description" to null, "ownerId" to ownerId),
                    ),
                ),
        )
        return objectMapper.readTree(result.response.contentAsString).get("id").asText()
    }

    @Test
    fun `creates a task within a project`() {
        val projectId = seedProject()

        val started = mockMvc.perform(
            post("/projects/$projectId/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("title" to "Design LM", "description" to null, "status" to "TODO", "assigneeId" to null),
                    ),
                ),
        ).andExpect(request().asyncStarted()).andReturn()

        mockMvc.perform(asyncDispatch(started))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("Design LM"))
            .andExpect(jsonPath("$.status").value("TODO"))
            .andExpect(jsonPath("$.projectId").value(projectId))
    }

    @Test
    fun `creating a task on missing project returns 404`() {
        val started = mockMvc.perform(
            post("/projects/00000000-0000-0000-0000-000000000000/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("title" to "x", "description" to null, "status" to "TODO", "assigneeId" to null),
                    ),
                ),
        ).andExpect(request().asyncStarted()).andReturn()

        mockMvc.perform(asyncDispatch(started))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value(404))
    }

    @Test
    fun `lists tasks filtered by status`() {
        val projectId = seedProject()
        listOf("TODO" to "a", "IN_PROGRESS" to "b", "DONE" to "c").forEach { (status, title) ->
            mockMvc.dispatch(
                post("/projects/$projectId/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf("title" to title, "description" to null, "status" to status, "assigneeId" to null),
                        ),
                    ),
            )
        }

        val started = mockMvc.perform(get("/projects/$projectId/tasks?status=IN_PROGRESS"))
            .andExpect(request().asyncStarted())
            .andReturn()
        mockMvc.perform(asyncDispatch(started))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$[0].title").value("b"))
    }
}
