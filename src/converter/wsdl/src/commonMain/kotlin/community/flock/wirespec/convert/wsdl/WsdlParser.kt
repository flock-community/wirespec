package community.flock.wirespec.convert.wsdl

import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.convert.wsdl.bindings.Definitions
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.serialization.XML

object WsdlParser {

    data class
    fun parse(xml: String) {

        val res = XML { autoPolymorphic = true }.decodeFromString<Definitions>(xml)
        val complexTypes = res.types
            .flatMap { it.schema }
            .flatMap { it.complexType }
            .associateBy { it.name }
        res.types
            .flatMap { it.schema }
            .flatMap { it.element }
            .map {
                it.complexType
                Type(
                    comment = null,
                    identifier = Identifier(it.name),
                    shape = Type.Shape(
                        value = emptyList()
                    )
                )


            }
        println(res)
    }

}

