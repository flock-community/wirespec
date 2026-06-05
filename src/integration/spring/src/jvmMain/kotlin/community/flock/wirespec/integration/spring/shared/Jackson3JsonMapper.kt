package community.flock.wirespec.integration.spring.shared

import tools.jackson.databind.ObjectMapper

/**
 * [WirespecJsonMapper] backed by a Jackson 3 (`tools.jackson`) ObjectMapper.
 * Used on Spring Boot 4.
 */
class Jackson3JsonMapper(private val jsonMapper: ObjectMapper) : WirespecJsonMapper {
    override fun readTree(bytes: ByteArray): Any = jsonMapper.readTree(bytes)
    override fun writeValueAsBytes(value: Any): ByteArray = jsonMapper.writeValueAsBytes(value)
}
