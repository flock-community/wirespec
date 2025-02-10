package community.flock.wirespec.convert.wsdl.bindings

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlCData
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

object JMSBindings {

    @Serializable
    @XmlSerialName(
        value = "binding",
        namespace = "http://www.w3.org/2008/07/soap/bindings/JMS/"
    )
    data class Binding(
        val messageFormat: String,
    ): WsdlBindings.Binding

    @Serializable
    @XmlSerialName(
        value = "connectionFactory",
        namespace = "http://www.w3.org/2008/07/soap/bindings/JMS/"
    )
    data class ConnectionFactory(
        @XmlValue(true)
        val data: String? = null,
    ): WsdlBindings.ConnectionFactory

    @Serializable
    @XmlSerialName(
        value = "targetAddress",
        namespace = "http://www.w3.org/2008/07/soap/bindings/JMS/"
    )
    data class TargetAddress(
        val destination: String,
        @XmlValue(true)
        val cData: String? = null,
    )
}