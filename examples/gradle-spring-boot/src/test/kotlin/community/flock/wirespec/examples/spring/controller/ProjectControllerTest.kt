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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ProjectControllerTest {

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

    @Test
    fun `creates listing and fetching projects`() {
        val createBody = objectMapper.writeValueAsString(
            mapOf("ref" to "apollo", "name" to "Apollo", "description" to "Moon", "ownerId" to ownerId),
        )

        val createStarted = mockMvc.perform(
            post("/projects").contentType(MediaType.APPLICATION_JSON).content(createBody),
        ).andExpect(request().asyncStarted()).andReturn()

        val createResult = mockMvc.perform(asyncDispatch(createStarted))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Apollo"))
            .andExpect(jsonPath("$.ownerId").value(ownerId))
            .andReturn()

        val createdId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

        val listStarted = mockMvc.perform(get("/projects")).andExpect(request().asyncStarted()).andReturn()
        val listResult = mockMvc.perform(asyncDispatch(listStarted))
            .andExpect(status().isOk)
            .andReturn()
        val ids = objectMapper.readTree(listResult.response.contentAsString).map { it.get("id").asText() }
        kotlin.test.assertTrue(ids.contains(createdId), "Expected $createdId in $ids")

        val getStarted = mockMvc.perform(get("/projects/$createdId")).andExpect(request().asyncStarted()).andReturn()
        mockMvc.perform(asyncDispatch(getStarted))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Apollo"))
    }

    @Test
    fun `updates an existing project`() {
        val createResult = mockMvc.dispatch(
            post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("ref" to "apollo", "name" to "Apollo", "description" to null, "ownerId" to ownerId),
                    ),
                ),
        )
        val createdId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

        val updateStarted = mockMvc.perform(
            put("/projects/$createdId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("ref" to "artemis", "name" to "Artemis", "description" to "successor", "ownerId" to ownerId),
                    ),
                ),
        ).andExpect(request().asyncStarted()).andReturn()

        mockMvc.perform(asyncDispatch(updateStarted))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Artemis"))
            .andExpect(jsonPath("$.description").value("successor"))
    }

    @Test
    fun `404 on missing project`() {
        val started = mockMvc.perform(get("/projects/00000000-0000-0000-0000-000000000000"))
            .andExpect(request().asyncStarted())
            .andReturn()
        mockMvc.perform(asyncDispatch(started))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value(404))
    }

    @Test
    fun `delete removes project`() {
        val createResult = mockMvc.dispatch(
            post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("ref" to "todelete", "name" to "ToDelete", "description" to null, "ownerId" to ownerId),
                    ),
                ),
        )
        val createdId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

        val deleteStarted = mockMvc.perform(delete("/projects/$createdId"))
            .andExpect(request().asyncStarted())
            .andReturn()
        mockMvc.perform(asyncDispatch(deleteStarted))
            .andExpect(status().isNoContent)

        val getStarted = mockMvc.perform(get("/projects/$createdId"))
            .andExpect(request().asyncStarted())
            .andReturn()
        mockMvc.perform(asyncDispatch(getStarted))
            .andExpect(status().isNotFound)
    }
}
