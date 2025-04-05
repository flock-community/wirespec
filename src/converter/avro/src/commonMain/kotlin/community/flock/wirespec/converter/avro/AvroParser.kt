package community.flock.wirespec.converter.avro

import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.converter.avro.AvroConverter.flatten
import kotlinx.serialization.json.Json

object AvroParser {

    fun parse(moduleContent: ModuleContent, strict: Boolean = true): AST {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        val avro = json.decodeFromString<AvroModel.Type>(moduleContent.content)
        val result: List<Definition> = avro.flatten() + when (avro) {
            is AvroModel.RecordType -> Channel(
                comment = null,
                identifier = DefinitionIdentifier(name = avro.name),
                reference = Reference.Custom(
                    value = avro.name,
                    isNullable = false,
                ),
            )
            is AvroModel.ArrayType -> TODO()
            is AvroModel.EnumType -> TODO()
            is AvroModel.SimpleType -> TODO()
            is AvroModel.LogicalType -> TODO()
            is AvroModel.MapType -> TODO()
            is AvroModel.UnionType -> TODO()
        }
        return AST(nonEmptyListOf(Module(moduleContent.src, result.toNonEmptyListOrNull() ?: error("Cannot yield non empty AST"))))
    }
}
