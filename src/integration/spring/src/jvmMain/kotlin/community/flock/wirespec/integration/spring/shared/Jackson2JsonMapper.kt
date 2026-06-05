package community.flock.wirespec.integration.spring.shared

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * [WirespecJsonMapper] backed by a Jackson 2 (`com.fasterxml.jackson`) ObjectMapper.
 * Used on Spring Boot 3.
 */
class Jackson2JsonMapper(private val objectMapper: ObjectMapper) : WirespecJsonMapper {
    override fun readTree(bytes: ByteArray): Any = objectMapper.readTree(bytes)
    override fun writeValueAsBytes(value: Any): ByteArray = objectMapper.writeValueAsBytes(value)
}
