package community.flock.wirespec.integration.spring.kotlin.it

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.integration.spring.kotlin.application.Application
//import community.flock.wirespec.integration.spring.application.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc

//import community.flock.wirespec.integration.spring.generated.Pet
import org.junit.jupiter.api.Test

@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc
class IntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

//    @Autowired
//    lateinit var service: Service

    @Test
    fun crudIntegrationTest() {

//        val pet = Pet(
//            id = 1,
//            name = "Dog",
//            photoUrls = listOf()
//        )
//
//        mockMvc
//            .perform(post("/pet")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(pet)))
//            .asyncDispatch()
//            .andDo(print())
//            .andExpect(status().isOk())
//            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Dog"))
//
//        mockMvc
//            .perform(put("/pet")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(pet.copy(name = "Cat"))))
//            .asyncDispatch()
//            .andDo(print())
//            .andExpect(status().isOk())
//            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Cat"))
//
//        mockMvc
//            .perform(get("/pet/${pet.id}"))
//            .asyncDispatch()
//            .andDo(print())
//            .andExpect(status().isOk())
//            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Cat"))
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
//    fun ResultActions.asyncDispatch():ResultActions = mockMvc.perform(asyncDispatch(this.andReturn()))

    }
}

