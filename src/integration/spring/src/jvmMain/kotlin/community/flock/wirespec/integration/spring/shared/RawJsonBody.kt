package community.flock.wirespec.integration.spring.shared

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.annotation.JsonValue

public class RawJsonBody(
    @get:JsonValue
    @get:JsonRawValue
    public val json: String,
) {
    public constructor(bytes: ByteArray) : this(String(bytes))
}
