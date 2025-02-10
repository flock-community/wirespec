package community.flock.wirespec.convert.wsdl.bindings

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

object SchemaBindings {
     @Serializable
     @XmlSerialName(
         value = "schema",
         namespace = "http://www.w3.org/2001/XMLSchema",
     )
     data class Schema(
         val targetNamespace: String? = null,
         val elementFormDefault: String? = null,
         val version: String? = null,
         val element: List<Element> = emptyList(),
         val simpleType: List<SimpleType> = emptyList(),
         val complexType: List<ComplexType> = emptyList(),
         val import: List<Import> = emptyList()
     )

     @Serializable
     @XmlSerialName(
         value = "import",
         namespace = "http://www.w3.org/2001/XMLSchema"
     )
     data class Import(
         val namespace: String,
         val schemaLocation: String,
     )

     @Serializable
     @XmlSerialName(
         value = "element",
         namespace = "http://www.w3.org/2001/XMLSchema"
     )
     data class Element(
         val name: String,
         val type: String? = null,
         val default: String? = null,
         val form: String? = null,
         val minOccurs: String? = "1",
         val maxOccurs: String? = "1",
         val nillable: Boolean? = null,
         val complexType: List<ComplexType> = emptyList(),
         val annotation: List<Annotation> = emptyList()
     )

     @Serializable
     @XmlSerialName(
         value = "annotation",
         namespace = "http://www.w3.org/2001/XMLSchema"
     )
     data class Annotation(
         val documentation: List<Documentation> = emptyList()
     )

     @Serializable
     @XmlSerialName(
         value = "documentation",
         namespace = "http://www.w3.org/2001/XMLSchema"
     )
     data class Documentation(
         @XmlValue val value: String? = null
     )

     @Serializable
     @XmlSerialName(
         value = "complexType",
         namespace = "http://www.w3.org/2001/XMLSchema"
     )
     data class ComplexType(
         val name: String? = null,
         val sequence: List<Sequence> = emptyList(),
         val complexContent: List<ComplexContent> = emptyList(),
         val attribute: List<Attribute> = emptyList(),
         val choice: List<Choice> = emptyList(),
         val annotation: List<Annotation> = emptyList()
     )

    @Serializable
    @XmlSerialName(
        value = "choice",
        namespace = "http://www.w3.org/2001/XMLSchema"
    )
    data class Choice(
        val element: List<Element> = emptyList()
    )

    @Serializable
    @XmlSerialName(
        value = "complexContent",
        namespace = "http://www.w3.org/2001/XMLSchema"
    )
    data class ComplexContent(
        val extension: List<Extension> = emptyList()
    )

    @Serializable
    @XmlSerialName(
        value = "extension",
        namespace = "http://www.w3.org/2001/XMLSchema"
    )
    data class Extension(
        val base: String,
        val sequence: List<Sequence> = emptyList()
    )

     @Serializable
     @XmlSerialName(
         value = "simpleType",
         namespace = "http://www.w3.org/2001/XMLSchema"
     )
     data class SimpleType(
         val name: String? = null,
         val restriction: List<Restriction> = emptyList(),
         val attribute: List<Attribute> = emptyList(),
         val annotation: List<Annotation> = emptyList(),
     )

     @Serializable
     @XmlSerialName(
         value = "restriction",
         namespace = "http://www.w3.org/2001/XMLSchema"
     )
     data class Restriction(
         val base: String? = null,
         val enumeration: List<Enumeration> = emptyList(),
     )

     @Serializable
     @XmlSerialName(
         value = "enumeration",
         namespace = "http://www.w3.org/2001/XMLSchema"
     )
     data class Enumeration(
         val value: String
     )

     @Serializable
     @XmlSerialName(
         value = "attribute",
         namespace = "http://www.w3.org/2001/XMLSchema"
     )
     data class Attribute(
         val name: String,
         val type: String,
     )

     @Serializable
     @XmlSerialName(
         value = "sequence",
         namespace = "http://www.w3.org/2001/XMLSchema"
     )
     data class Sequence(
         val element: List<Element>,
     )
 }