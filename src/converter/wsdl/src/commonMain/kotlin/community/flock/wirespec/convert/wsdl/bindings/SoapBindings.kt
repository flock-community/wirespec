package community.flock.wirespec.convert.wsdl.bindings

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName(
    value = "binding",
    namespace = "http://schemas.xmlsoap.org/wsdl/soap/"
)
data class SoapBinding(
    val transport: String,
    val style: String? = null,
)

@Serializable
@XmlSerialName(
    value = "binding",
    namespace = "http://schemas.xmlsoap.org/wsdl/soap12/"
)
data class Soap12Binding(
    val transport: String,
    val style: String? = null,
)


@Serializable
@XmlSerialName(
    value = "operation",
    namespace = "http://schemas.xmlsoap.org/wsdl/soap/"
)
data class SoapOperation(
    val soapAction: String,
    val style: String,
): Operation

@Serializable
@XmlSerialName(
    value = "operation",
    namespace = "http://schemas.xmlsoap.org/wsdl/soap12/"
)
data class Soap12Operation(
    val soapAction: String,
    val style: String,
): Operation


@Serializable
@XmlSerialName(
    value = "body",
    namespace = "http://schemas.xmlsoap.org/wsdl/soap/"
)
data class SoapBody(
    val use: String,
): Body

@Serializable
@XmlSerialName(
    value = "body",
    namespace = "http://schemas.xmlsoap.org/wsdl/soap12/"
)
data class Soap12Body(
    val use: String,
): Body

@Serializable
@XmlSerialName(
    value = "address",
    namespace = "http://schemas.xmlsoap.org/wsdl/soap/"
)
data class SoapAddress(
    val location: String,
) : Address

@Serializable
@XmlSerialName(
    value = "address",
    namespace = "http://schemas.xmlsoap.org/wsdl/soap12/"
)
data class Soap12Address(
    val location: String,
) : Address
