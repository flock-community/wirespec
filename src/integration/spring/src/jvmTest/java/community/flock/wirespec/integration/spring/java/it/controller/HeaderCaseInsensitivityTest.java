package community.flock.wirespec.integration.spring.java.it.controller;

import community.flock.wirespec.integration.spring.java.application.Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test for RFC 7230 compliance - HTTP header names should be case-insensitive
 * https://github.com/flock-community/wirespec/issues/510
 */
@SpringBootTest(classes = {Application.class})
@AutoConfigureMockMvc
public class HeaderCaseInsensitivityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldAcceptApiKeyHeaderWithExactCaseFromWirespecDefinition() throws Exception {
        MvcResult result = mockMvc
            .perform(
                delete("/pet/1")
                    .header("api_key", "test-key-123")
            )
            .andReturn();

        mockMvc.perform(asyncDispatch(result))
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldAcceptApiKeyHeaderWithLowercase() throws Exception {
        MvcResult result = mockMvc
            .perform(
                delete("/pet/1")
                    .header("api_key", "test-key-123")
            )
            .andReturn();

        mockMvc.perform(asyncDispatch(result))
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldAcceptApiKeyHeaderWithUppercase() throws Exception {
        MvcResult result = mockMvc
            .perform(
                delete("/pet/1")
                    .header("API_KEY", "test-key-123")
            )
            .andReturn();

        mockMvc.perform(asyncDispatch(result))
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldAcceptApiKeyHeaderWithMixedCase() throws Exception {
        MvcResult result = mockMvc
            .perform(
                delete("/pet/1")
                    .header("Api_Key", "test-key-123")
            )
            .andReturn();

        mockMvc.perform(asyncDispatch(result))
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldAcceptApiKeyHeaderWithDifferentMixedCase() throws Exception {
        MvcResult result = mockMvc
            .perform(
                delete("/pet/1")
                    .header("API-KEY", "test-key-123")
            )
            .andReturn();

        mockMvc.perform(asyncDispatch(result))
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldAcceptStandardContentTypeHeaderWithUppercase() throws Exception {
        // Content-Type is commonly sent with mixed case
        MvcResult result = mockMvc
            .perform(
                delete("/pet/1")
                    .header("CONTENT-TYPE", "application/json")
            )
            .andReturn();

        mockMvc.perform(asyncDispatch(result))
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldAcceptStandardContentTypeHeaderWithLowercase() throws Exception {
        // Many HTTP clients normalize headers to lowercase
        MvcResult result = mockMvc
            .perform(
                delete("/pet/1")
                    .header("content-type", "application/json")
            )
            .andReturn();

        mockMvc.perform(asyncDispatch(result))
            .andDo(print())
            .andExpect(status().isBadRequest());
    }
}
