package community.flock.wirespec.convert.avro

import kotlinx.serialization.json.Json

object AvroParser {
    fun parse(schemaContent: String): AvroModel.Type =
        Json.decodeFromString<AvroModel.Type>(schemaContent)
}
