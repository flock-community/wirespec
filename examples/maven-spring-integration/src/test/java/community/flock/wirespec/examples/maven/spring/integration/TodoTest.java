package community.flock.wirespec.examples.maven.spring.integration;

import community.flock.wirespec.examples.maven.spring.integration.service.TodoService;
import community.flock.wirespec.generated.examples.spring.model.Todo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@SpringBootTest
@AutoConfigureMockMvc
class TodoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TodoService service;

    @Test
    void shouldReturnDefaultMessage() throws Exception {
        MvcResult mvcGetResult = mockMvc
                .perform(get("/todos"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcGetResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        MvcResult mvcPostResult = mockMvc
                .perform(post("/todos")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(
                                //language=json
                                """
                                        {
                                          "name": "test",
                                          "done": true
                                        }
                                        """))
                .andExpect(request().asyncStarted())
                .andReturn();

        this.mockMvc.perform(asyncDispatch(mvcPostResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", equalTo("test")))
                .andExpect(jsonPath("$.done", equalTo(true)));

    }

    @Test
    void shouldUploadAttachment() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "plain",
                "hello.txt",
                "text/plain",
                "Hello Wirespec".getBytes()
        );

        MockMultipartFile csv = new MockMultipartFile(
                "csv",
                "hello.csv",
                "text/csv",
                "id,name,done\n1,'todo 1',true\n2,'todo 2',false".getBytes()
        );

        MockMultipartFile json = new MockMultipartFile(
                "json",
                "hello.json",
                "application/json",
                "{\"id\": 1, \"name\": \"Todo 1\", \"done\": false}".getBytes()

        );

        MvcResult mvcMultipartResult = mockMvc
                .perform(multipart("/todos/{id}/upload", "1")
                        .file(file)
                        .file(json)
                        .file(csv)
                        .contentType(MULTIPART_FORM_DATA_VALUE))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcMultipartResult))
                .andExpect(status().isCreated());
        byte[] files = (byte[]) service.files.get("plain");
        Todo todo = (Todo) service.files.get("json");
        assertEquals("Hello Wirespec", new String(files));
        assertEquals("Todo 1", todo.name());
    }
}
