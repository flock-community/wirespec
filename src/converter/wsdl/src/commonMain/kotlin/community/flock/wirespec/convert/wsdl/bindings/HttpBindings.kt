package community.flock.wirespec.convert.wsdl.bindings

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

object HttpBindings {
    @Serializable
    @XmlSerialName(
        value = "binding",
        namespace = "http://schemas.xmlsoap.org/wsdl/http/"
    )
    data class HttpBinding(
        val verb: String,
    ):WsdlBindings.Binding

    @Serializable
    @XmlSerialName(
        value = "operation",
        namespace = "http://schemas.xmlsoap.org/wsdl/http/"
    )
    data class HttpOperation(
        val location: String,
    ): WsdlBindings.Operation

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
    ): WsdlBindings.Address
}