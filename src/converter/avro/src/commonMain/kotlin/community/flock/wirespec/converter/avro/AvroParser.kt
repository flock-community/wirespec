package community.flock.wirespec.converter.avro

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.converter.avro.AvroConverter.flatten
import kotlinx.serialization.json.Json

object AvroParser {

    fun parse(schemaContent: String, strict: Boolean = true): AST {
        val json = Json{ignoreUnknownKeys = true; isLenient = true}
        val avro = json.decodeFromString<AvroModel.Type>(schemaContent)
        return avro.flatten() + when(avro){
            is AvroModel.RecordType -> Channel(
                comment = null,
                identifier = DefinitionIdentifier(avro.name),
                isNullable = false,
                reference = Reference.Custom(
                    value = avro.name,
                    isIterable = false,
                    isDictionary = false
                )
            )
            is AvroModel.ArrayType -> TODO()
            is AvroModel.EnumType -> TODO()
            is AvroModel.SimpleType -> TODO()
            is AvroModel.LogicalType -> TODO()
        }
    }
}