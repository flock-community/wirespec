package community.flock.wirespec.convert.wsdl.bindings

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName(
    value = "binding",
    namespace = "http://schemas.xmlsoap.org/wsdl/http/"
)
data class HttpBinding(
    val verb: String,
)

@Serializable
@XmlSerialName(
    value = "operation",
    namespace = "http://schemas.xmlsoap.org/wsdl/http/"
)
data class HttpOperation(
    val location: String,
): Operation

@Serializable
@XmlSerialName(
    value = "urlEncoded",
    namespace = "http://schemas.xmlsoap.org/wsdl/http/"
)
data object UrlEncoded

@Serializable
@XmlSerialName(
    value = "address",
    namespace = "http://schemas.xmlsoap.org/wsdl/http/"
)
data class HttpAddress(
    val location: String,
) : Address
