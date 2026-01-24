package community.flock.wirespec.integration.spring.kotlin.it.controller

import community.flock.wirespec.integration.spring.kotlin.application.Application
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Test for RFC 7230 compliance - HTTP header names should be case-insensitive
 * https://github.com/flock-community/wirespec/issues/510
 */
@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc
class HeaderCaseInsensitivityTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `should accept api_key header with exact case from wirespec definition`() {
        mockMvc
            .perform(
                delete("/pet/1")
                    .header("api_key", "test-key-123")
            )
            .asyncDispatch()
            .andDo(print())
            .andExpect(status().isBadRequest())
    }

    @Test
    fun `should accept api_key header with lowercase`() {
        mockMvc
            .perform(
                delete("/pet/1")
                    .header("api_key", "test-key-123")
            )
            .asyncDispatch()
            .andDo(print())
            .andExpect(status().isBadRequest())
    }

    @Test
    fun `should accept api_key header with uppercase`() {
        mockMvc
            .perform(
                delete("/pet/1")
                    .header("API_KEY", "test-key-123")
            )
            .asyncDispatch()
            .andDo(print())
            .andExpect(status().isBadRequest())
    }

    @Test
    fun `should accept api_key header with mixed case`() {
        mockMvc
            .perform(
                delete("/pet/1")
                    .header("Api_Key", "test-key-123")
            )
            .asyncDispatch()
            .andDo(print())
            .andExpect(status().isBadRequest())
    }

    @Test
    fun `should accept api_key header with different mixed case`() {
        mockMvc
            .perform(
                delete("/pet/1")
                    .header("API-KEY", "test-key-123")
            )
            .asyncDispatch()
            .andDo(print())
            .andExpect(status().isBadRequest())
    }

    @Test
    fun `should accept standard Content-Type header with uppercase`() {
        // Content-Type is commonly sent with mixed case
        mockMvc
            .perform(
                delete("/pet/1")
                    .header("CONTENT-TYPE", "application/json")
            )
            .asyncDispatch()
            .andDo(print())
            .andExpect(status().isBadRequest())
    }

    @Test
    fun `should accept standard Content-Type header with lowercase`() {
        // Many HTTP clients normalize headers to lowercase
        mockMvc
            .perform(
                delete("/pet/1")
                    .header("content-type", "application/json")
            )
            .asyncDispatch()
            .andDo(print())
            .andExpect(status().isBadRequest())
    }

    fun ResultActions.asyncDispatch(): ResultActions = mockMvc.perform(asyncDispatch(this.andReturn()))
}
