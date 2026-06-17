package community.flock.wirespec.integration.spring.shared

/**
 * Version-neutral JSON mapper used by the multipart request path, so the Spring
 * integration works against either Jackson 2 (Spring Boot 3) or Jackson 3 (Spring Boot 4).
 *
 * Implementations wrap a concrete Jackson `ObjectMapper`. Returned tree nodes are opaque
 * (`Any`) and must be written back with the same mapper that produced them.
 */
interface WirespecJsonMapper {
    fun readTree(bytes: ByteArray): Any
    fun writeValueAsBytes(value: Any): ByteArray
}
