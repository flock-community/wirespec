package community.flock.wirespec.convert.wsdl

import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.convert.wsdl.bindings.SchemaBindings
import community.flock.wirespec.convert.wsdl.bindings.WsdlBindings
import community.flock.wirespec.convert.wsdl.bindings.WsdlBindings.Definitions
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.serialization.XML

object WsdlParser {

    fun parseSchema(xml: String, path: Path): AST {
        val res = XML { autoPolymorphic = true }.decodeFromString<SchemaBindings.Schema>(xml)

        val import = res.import
            .flatMap {
                it.schemaLocation
                    ?.let { location ->
                        val file = SystemFileSystem.resolve(Path(path, location))
                        val data = SystemFileSystem.source(file).buffered().readString()
                        parseSchema(data, file.parent ?: error("Missing schema"))
                    }
                    ?: emptyList()
            }

        val simpleTypes = res.simpleType
            .map { it.toType() }


        val complexTypes = res.complexType
            .map { it.toType() }

        return import + simpleTypes + complexTypes
    }

    fun parseDefinitions(xml: String, path: Path): AST {
        val res = XML { autoPolymorphic = true }.decodeFromString<Definitions>(xml)

        val endpoints = res.portType
            .flatMap { it.operation }
            .map { it.toEndpoint() }

        val importWsdl = res.import
            .flatMap {
                val file = SystemFileSystem.resolve(Path(path, it.location))
                val data = SystemFileSystem.source(file).buffered().readString()
                parseDefinitions(data, file.parent ?: error("Missing schema"))
            }

        val importSchema = res.types
            .flatMap { it.schema }
            .flatMap { it.import }
            .flatMap {
                it.schemaLocation
                    ?.let { location ->
                        val file = SystemFileSystem.resolve(Path(path, location))
                        val data = SystemFileSystem.source(file).buffered().readString()
                        parseSchema(data, file.parent ?: error("Missing schema"))
                    }
                    ?: emptyList()

            }

        val aliasMap = res.types
            .flatMap { it.schema }
            .flatMap { it.element }
            .associate { it.name to it.type?.split(":")?.last() }

        val messages = res.message.map { it.toType(aliasMap) }

        val simpleTypes = res.types
            .flatMap { it.schema }
            .flatMap { it.simpleType }
            .map { it.toType() }

        val complexTypes = res.types
            .flatMap { it.schema }
            .flatMap { it.complexType }
            .map { it.toType() }

        val elementTypes = res.types
            .flatMap { it.schema }
            .flatMap { it.element }
            .flatMap { elm -> elm.complexType.map { it.toType(elm.name) } }

        return endpoints + importWsdl + importSchema + messages + simpleTypes + complexTypes + elementTypes
    }

    private fun WsdlBindings.Message.toType(aliasMap:Map<String,String?>) =
        Type(
            comment = null,
            identifier = DefinitionIdentifier(name),
            shape = Type.Shape(
                value = part.map { part ->
                    Field(
                        identifier = FieldIdentifier(part.name),
                        reference = part.element?.split(":")?.last()
                            ?.let { Reference.Custom(aliasMap[it]?:it, false, false) }
                            ?: Reference.Unit(false, false),
                        isNullable = false
                    )
                }
            ),
            extends = emptyList()

        )


    fun SchemaBindings.SimpleType.toType(name: String? = null) = when {
        restriction.isNotEmpty() -> Enum(
            comment = null,
            identifier = DefinitionIdentifier(this.name?.firstToUpper() ?: name ?: "NO_NAME"),
            entries = restriction.flatMap { it.enumeration.map { it.value } }.toSet()
        )

        else -> TODO("Cannot read SimpleType")
    }

    fun SchemaBindings.ComplexType.toType(name: String? = null) = Type(
        comment = null,
        identifier = DefinitionIdentifier(this.name ?: name ?: "NO_NAME"),
        shape = Type.Shape(sequence.flatMap { it.element }.map { it.toField() }),
        extends = emptyList()
    )

    fun SchemaBindings.Element.toField(): Field {
        val isIterable = this.maxOccurs != "1"
        return Field(
            identifier = FieldIdentifier(this.name),
            isNullable = this.nillable ?: this.minOccurs?.let { it == "0" } ?: false,
            reference = when (val t = type?.split(":")?.last()) {
                "decimal" -> Reference.Primitive(
                    type = Reference.Primitive.Type.Number(Reference.Primitive.Type.Precision.P64),
                    isIterable = isIterable
                )

                "string", "date", "dateTime" -> Reference.Primitive(
                    type = Reference.Primitive.Type.String,
                    isIterable = isIterable
                )

                "int", "integer", "positiveInteger" -> Reference.Primitive(
                    type = Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P32),
                    isIterable = isIterable
                )

                "base64Binary" -> Reference.Primitive(type = Reference.Primitive.Type.String, isIterable = isIterable)
                "anyType" -> Reference.Any(isIterable = isIterable)
                null -> Reference.Unit(isIterable = isIterable)
                else -> Reference.Custom(value = t, isIterable = isIterable)
            }
        )
    }

    fun WsdlBindings.WsdlOperation.toEndpoint() =
        Endpoint(
            comment = null,
            identifier = DefinitionIdentifier(this.name + "Endpoint"),
            method = Endpoint.Method.POST,
            path = listOf(Endpoint.Segment.Literal(this.name)),
            queries = emptyList(),
            headers = emptyList(),
            cookies = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = input.first().message
                            ?.let {
                                Reference.Custom(
                                    value = it.split(":").last(),
                                    isIterable = false,
                                    isDictionary = false
                                )
                            }
                            ?: Reference.Unit(
                                isIterable = false,
                                isDictionary = false
                            )
                    )
                )
            ),
            responses = listOf(
                Endpoint.Response(
                    status = "200",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = output.first().message
                            ?.let {
                                Reference.Custom(
                                    value = it.split(":").last(),
                                    isIterable = false,
                                    isDictionary = false
                                )
                            }
                            ?: Reference.Unit(
                                isIterable = false,
                                isDictionary = false
                            )
                    )
                )

            )
        )

}

