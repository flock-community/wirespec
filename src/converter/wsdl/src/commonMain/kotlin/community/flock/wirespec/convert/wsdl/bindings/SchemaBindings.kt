package community.flock.wirespec.convert.wsdl.bindings

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName(
    value = "schema",
    namespace = "http://www.w3.org/2001/XMLSchema",
)
data class Schema(
    val targetNamespace: String,
    val elementFormDefault: String? = null,
    val element: List<Element> = emptyList(),
    val complexType: List<ComplexType> = emptyList()
)

@Serializable
@XmlSerialName(
    value = "element",
    namespace = "http://www.w3.org/2001/XMLSchema"
)
data class Element(
    val name: String,
    val type: String? = null,
    val minOccurs: String? = "1",
    val maxOccurs: String? = "1",
    val nillable: Boolean? = null,
    val complexType: List<ComplexType> = emptyList()
)

@Serializable
@XmlSerialName(
    value = "complexType",
    namespace = "http://www.w3.org/2001/XMLSchema"
)
data class ComplexType(
    val name: String? = null,
    val sequence: List<Sequence> = emptyList(),
)

@Serializable
@XmlSerialName(
    value = "sequence",
    namespace = "http://www.w3.org/2001/XMLSchema"
)
data class Sequence(
    val element: List<Element>,
)