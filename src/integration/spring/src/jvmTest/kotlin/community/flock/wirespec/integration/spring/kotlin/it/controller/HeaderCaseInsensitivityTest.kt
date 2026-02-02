package community.flock.wirespec.integration.spring.kotlin.it.controller

import community.flock.wirespec.integration.spring.kotlin.application.Application
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Test for RFC 7230 compliance - HTTP header names should be case-insensitive
 */
@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc
class HeaderCaseInsensitivityTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @ParameterizedTest
    @CsvSource(
        "X-Request-ID,RandomHeader",
        "x-REquEst-iD,RanDoMHeADer",
        "X-REQUEST-ID,RANDOMHEADER",
        "x-request-id,randomheader",
    )
    fun `Incoming headers should be parsed case insensitively`(requestIdHeader: String, randomHeader: String) {
        val body = """{"number": 1, "string": "test"}"""

        val perform = mockMvc
            .perform(
                post("/api/parrot")
                    .header(requestIdHeader, "request-id")
                    .header(randomHeader, "random-value")
                    .contentType("application/json")
                    .content(body),
            )

        asyncDispatch(perform)
            .andExpect(status().isOk())
            .andExpect(header("X-REQUEST-ID", "request-id"))
            .andExpect(header("RANDOMHEADER", "random-value"))
    }

    @Throws(Exception::class)
    private fun asyncDispatch(resultActions: ResultActions): ResultActions = if (resultActions.andReturn().request.isAsyncStarted) {
        mockMvc.perform(asyncDispatch(resultActions.andReturn()))
    } else {
        resultActions
    }

    fun header(headerName: String, vararg expectedValues: String): ResultMatcher = ResultMatcher { result ->
        val response: MockHttpServletResponse = result.response

        val headerValues = response.getHeaders(headerName)
        headerValues.shouldContainExactly(*expectedValues)
    }
}
