package community.flock.wirespec.convert.wsdl.bindings

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlCData
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName(
    value = "definitions",
    namespace = "http://schemas.xmlsoap.org/wsdl/",
)
data class Definitions(
    val name: String? = null,
    val targetNamespace: String,
    val types: List<Types>,
    val message: List<Message>,
    val portType: List<PortType>,
    val binding: List<WsdlBinding>,
    val service: List<Service>
)

@Serializable
@XmlSerialName(
    value = "types",
    namespace = "http://schemas.xmlsoap.org/wsdl/"
)
data class Types(
    val schema: List<Schema>,
)



@Serializable
@XmlSerialName(
    value = "message",
    namespace = "http://schemas.xmlsoap.org/wsdl/",
)
data class Message(
    val name: String,
    val part: List<Part>,
)

@Serializable
@XmlSerialName(
    value = "part",
    namespace = "http://schemas.xmlsoap.org/wsdl/",
)
data class Part(
    val name: String,
    val type: String? = null,
    val element: String? = null,
)

@Serializable
@XmlSerialName(
    value = "portType",
    namespace = "http://schemas.xmlsoap.org/wsdl/",
)
data class PortType(
    val name: String,
    val operation: List<WsdlOperation>
)

@Serializable
@XmlSerialName(
    value = "input",
    namespace = "http://schemas.xmlsoap.org/wsdl/",
)
data class Input(
    val message: String? = null,
    val body: Body? = null,
    val urlEncoded: List<UrlEncoded> = emptyList(),
    val mimeContent: List<MimeContent> = emptyList(),
)

@Serializable
@XmlSerialName(
    value = "output",
    namespace = "http://schemas.xmlsoap.org/wsdl/",
)
data class Output(
    val message: String? = null,
    val body: Body? = null,
    val mimeXml: List<MimeXml> = emptyList(),
)

@Serializable
@XmlSerialName(
    value = "binding",
    namespace = "http://schemas.xmlsoap.org/wsdl/"
)
data class WsdlBinding(
    val name: String,
    val type: String,
    val binding: List<SoapBinding>,
    val binding12: List<Soap12Binding>,
    val bindingHttp: List<HttpBinding>,
    val operation: List<WsdlOperation>
)

@Serializable
@XmlSerialName(
    value = "operation",
    namespace = "http://schemas.xmlsoap.org/wsdl/"
)
data class WsdlOperation(
    val name: String,
    @XmlCData val documentation: String? = null,
    val operation: List<Operation> = emptyList(),
    val input: List<Input> = emptyList(),
    val output: List<Output> = emptyList(),
)

@Serializable
sealed interface Operation

@Serializable
@XmlSerialName(
    value = "mimeXml",
    namespace = "http://schemas.xmlsoap.org/wsdl/mime/"
)
data class MimeXml(
    val part: String
)

@Serializable
@XmlSerialName(
    value = "content",
    namespace = "http://schemas.xmlsoap.org/wsdl/mime/"
)
data class MimeContent(
    val type: String
)

@Serializable
sealed interface Body

@Serializable
@XmlSerialName(
    value = "service",
    namespace = "http://schemas.xmlsoap.org/wsdl/"
)
data class Service(
    val name: String,
    @XmlCData val documentation: String?,
    val port: List<Port>,
)

@Serializable
@XmlSerialName(
    value = "port",
    namespace = "http://schemas.xmlsoap.org/wsdl/"
)
data class Port(
    val name: String,
    val binding: String,
    val address: Address,
)

@Serializable
sealed interface Address

