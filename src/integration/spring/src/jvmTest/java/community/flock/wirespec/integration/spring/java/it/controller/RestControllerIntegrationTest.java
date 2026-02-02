package community.flock.wirespec.integration.spring.java.it.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import community.flock.wirespec.integration.spring.java.application.Application;
import community.flock.wirespec.integration.spring.java.application.Service;
import community.flock.wirespec.integration.spring.java.generated.model.Pet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
public class RestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Service service;

    @Test
    public void crudRestControllerIntegrationTest() throws Exception {
        Pet pet = new Pet(
            Optional.of(1L),
            "Dog",
            Optional.empty(),
            java.util.List.of(),
            Optional.empty(),
            Optional.empty()
        );

        asyncDispatch(mockMvc
            .perform(
                post("/pet")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(pet))
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Dog"));

        // Update
        Pet updatedPet = new Pet(
            pet.id(),
            "Cat",
            pet.category(),
            pet.photoUrls(),
            pet.tags(),
            pet.status()
        );

        asyncDispatch(mockMvc
            .perform(
                put("/pet")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updatedPet))
            ))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Cat"));

        asyncDispatch(mockMvc
            .perform(get("/pet/" + pet.id().get())))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Cat"));

        asyncDispatch(mockMvc
            .perform(delete("/pet/" + pet.id().get())))
            .andDo(print())
            .andExpect(status().isBadRequest());

        assertEquals(0, service.getList().size());
    }

    @Test
    public void queryParameters() throws Exception {
        asyncDispatch(mockMvc
            .perform(get("/pet/findByTags").param("tags", "Smilodon", "Dodo", "Mammoth")))
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    public void multipartFormData() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "filename.txt", "text/plain", "data".getBytes());
        MockMultipartFile metadata = new MockMultipartFile("additionalMetadata", "", "application/json", "\"metadata\"".getBytes());
        MockMultipartFile json = new MockMultipartFile("json", "", "application/json", "{\"foo\":\"bar\"}".getBytes());

        asyncDispatch(mockMvc
            .perform(
                multipart("/pet/1/uploadImage")
                    .file(file)
                    .file(metadata)
                    .file(json)
            ))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("metadata"));

        assertArrayEquals(service.getFiles().get(0), file.getBytes());
    }

    private ResultActions asyncDispatch(ResultActions resultActions) throws Exception {
        if (resultActions.andReturn().getRequest().isAsyncStarted()) {
            return mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(resultActions.andReturn()));
        }
        return resultActions;
    }
}
