package community.flock.wirespec.convert.wsdl

import com.fasterxml.jackson.core.io.IOContext
import com.fasterxml.jackson.dataformat.xml.XmlFactory
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.util.StaxUtil
import community.flock.wirespec.generated.CapitalCity
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.Writer
import java.util.Objects
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamWriter
import kotlin.test.Test


internal class NamespaceXmlFactory(defaultNamespace: String, prefix2Namespace: Map<String, String>) :
    XmlFactory() {
    private val defaultNamespace: String = Objects.requireNonNull(defaultNamespace)
    private val prefix2Namespace: Map<String, String> = Objects.requireNonNull(prefix2Namespace)

    override fun _createXmlWriter(ctxt: IOContext?, w: Writer?): XMLStreamWriter {
        val writer: XMLStreamWriter = super._createXmlWriter(ctxt, w)
        try {
            writer.setDefaultNamespace(defaultNamespace)
            for ((key, value) in prefix2Namespace) {
                writer.setPrefix(key, value)
            }
        } catch (e: XMLStreamException) {
            StaxUtil.throwAsGenerationException(e, null)
        }
        return writer
    }
}
class JacksonTest {

    val mapper: XmlMapper = XmlMapper.builder(NamespaceXmlFactory("web", mapOf("web" to "http://www.oorsprong.org/websamples.countryinfo")))
        .defaultUseWrapper(false) // enable/disable Features, change AnnotationIntrospector
        .build()

    @Test
    fun testJackson() {


//        val obj = CapitalCity(
//            listOf(
//                TCurrency("A", "1"),
//                TCurrency("B", "2"),
//                TCurrency("C", "3"),
//            )
//        )
//        val xml = mapper.writeValueAsString(obj)
//
//        println(xml)


    }

    @Test
    fun sendSoap() {
        fun soap(body:String) = """
            |<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:web="http://www.oorsprong.org/websamples.countryinfo">
            |   <soapenv:Header/>
            |   <soapenv:Body>
            |      ${body}
            |   </soapenv:Body>
            |</soapenv:Envelope>
        """.trimMargin()

        val obj = CapitalCity(
            sCountryISOCode = "NL"
        )

        val body = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj)

        println(body)
        println(soap(body))

        val TEXT_XML = "text/xml".toMediaType();
        val request: Request = Request.Builder()
            .url("http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso")
            .method("POST", RequestBody.create(TEXT_XML, soap(body)))
            .headers(
                Headers.Builder()
                    .add("SOAPAction", "")
                    .add("Host", "webservices.oorsprong.org")
                    .build())
            .build()

        val client = OkHttpClient()

        val res = client.newCall(request).execute().use { response ->
            response.body?.string()
        }

        println(res)
    }
}