package community.flock.wirespec.integration.spring.shared

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.annotation.JsonValue

class RawJsonBody(
    @get:JsonValue
    @get:JsonRawValue
    val json: String,
) {
    constructor(bytes: ByteArray) : this(String(bytes))
}
