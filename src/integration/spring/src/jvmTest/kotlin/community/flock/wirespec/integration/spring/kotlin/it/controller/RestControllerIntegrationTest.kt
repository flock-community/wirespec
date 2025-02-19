package community.flock.wirespec.integration.spring.kotlin.it.controller

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.integration.spring.kotlin.application.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@SpringBootTest
@AutoConfigureMockMvc
class RestControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var service: Service

//    @Test
//    fun `CRUD RestController integration test`() {
//
//        val pet = Pet(
//            id = 1,
//            name = "Dog",
//            photoUrls = listOf(),
//            category = null,
//            tags = null,
//            status = null,
//        )
//
//        mockMvc
//            .perform(
//                post("/pet")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(objectMapper.writeValueAsString(pet))
//            )
//            .asyncDispatch()
//            .andDo(print())
//            .andExpect(status().isOk())
//            .andExpect(jsonPath("$.name").value("Dog"))
//
//        mockMvc
//            .perform(
//                put("/pet")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(objectMapper.writeValueAsString(pet.copy(name = "Cat")))
//            )
//            .asyncDispatch()
//            .andDo(print())
//            .andExpect(status().isOk())
//            .andExpect(jsonPath("$.name").value("Cat"))
//
//        mockMvc
//            .perform(get("/pet/${pet.id}"))
//            .asyncDispatch()
//            .andDo(print())
//            .andExpect(status().isOk())
//            .andExpect(jsonPath("$.name").value("Cat"))
//
//
//        mockMvc
//            .perform(delete("/pet/${pet.id}"))
//            .asyncDispatch()
//            .andDo(print())
//            .andExpect(status().isBadRequest())
//
//        assertEquals(0, service.list.size)
//    }
//
//    @Test
//    fun `query parameters`() {
//        mockMvc
//            .perform(get("/pet/findByTags").param("tags", "Smilodon", "Dodo", "Mammoth"))
//            .asyncDispatch()
//            .andDo(print())
//            .andExpect(status().isOk())
//    }
//
//    fun ResultActions.asyncDispatch(): ResultActions = mockMvc.perform(asyncDispatch(this.andReturn()))
}
