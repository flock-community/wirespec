package community.flock.wirespec.examples.maven.spring.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TodoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnDefaultMessage() throws Exception {
        MvcResult mvcGetResult =  mockMvc
                .perform(get("/todos"))
                .andExpect(request().asyncStarted())
                .andReturn();

        this.mockMvc.perform(asyncDispatch(mvcGetResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        MvcResult mvcPostResult =  mockMvc
                .perform(post("/todos")
                                .contentType(APPLICATION_JSON_VALUE)
                                .content("{\"name\":\"test\", \"done\": true}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        this.mockMvc.perform(asyncDispatch(mvcPostResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", equalTo("test")))
                .andExpect(jsonPath("$.done", equalTo(true)));

    }
}
