package community.flock.wirespec.integration.spring.java.it.controller;

import community.flock.wirespec.integration.spring.java.application.Application;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test for RFC 7230 compliance - HTTP header names should be case-insensitive
 */
@SpringBootTest(classes = {Application.class})
@AutoConfigureMockMvc
public class HeaderCaseInsensitivityTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @CsvSource({
            "X-Request-ID,RandomHeader",
            "x-REquEst-iD,RanDoMHeADer",
            "X-REQUEST-ID,RANDOMHEADER",
            "x-request-id,randomheader"
    })
    public void shouldParseIncomingHeaderCaseInsensitive(String requestIdHeader, String randomHeader) throws Exception {
        String body = "{\"number\": 1, \"string\": \"test\"}";

        asyncDispatch(mockMvc
                .perform(
                        post("/api/parrot")
                                .header(requestIdHeader, "request-id")
                                .header(randomHeader, "random-value")
                                .contentType("application/json")
                                .content(body)
                ))
                .andExpect(status().isOk())
                .andExpect(header().string("X-REQUEST-ID", "request-id"))
                .andExpect(header().string("RANDOMHEADER", "random-value"));
    }

    private ResultActions asyncDispatch(ResultActions resultActions) throws Exception {
        if (resultActions.andReturn().getRequest().isAsyncStarted()) {
            return mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(resultActions.andReturn()));
        }
        return resultActions;
    }
}
