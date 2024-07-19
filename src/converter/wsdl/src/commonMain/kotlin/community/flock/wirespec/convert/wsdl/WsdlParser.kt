package community.flock.wirespec.convert.wsdl

import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.convert.wsdl.bindings.ComplexType
import community.flock.wirespec.convert.wsdl.bindings.Definitions
import community.flock.wirespec.convert.wsdl.bindings.Element
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.serialization.XML

object WsdlParser {

    fun parse(xml: String): AST {
        val res = XML { autoPolymorphic = true }.decodeFromString<Definitions>(xml)
        return res.types
            .flatMap { it.schema }
            .flatMap { it.complexType }
            .map { it.toType() }
    }

    fun ComplexType.toType() = Type(
        comment = null,
        identifier = Identifier(name ?: "NO_NAME"),
        shape = Type.Shape(sequence.flatMap { it.element }.map { it.toField() })
    )

    fun Element.toField(): Field{
        val isIterable = this.maxOccurs != "1"
        return Field(
            identifier = Identifier(this.name),
            isNullable = this.nillable ?: false,
            reference = when (val t = type?.split(":")?.last()) {
                "string" -> Field.Reference.Primitive(type = Field.Reference.Primitive.Type.String, isIterable = isIterable)
                null -> Field.Reference.Unit(isIterable = isIterable)
                else -> Field.Reference.Custom(value = t.firstToUpper(), isIterable = isIterable)
            }
        )
    }


}

