package community.flock.wirespec.convert.wsdl

import com.goncalossilva.resources.Resource
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.shared.KotlinShared
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.utils.noLogger
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test

class WsdlParserTest {

    val base = Path("src/commonTest/resources")

    @Test
    fun ingServiceRequest() {
        val path = Path(base, "srv/ServiceRequest")
        val file = Path(path, "GCB_GINT_ServiceRequest_002.wsdl")
        val source = SystemFileSystem.source(file).buffered()
        val ast = WsdlParser
            .parseDefinitions(source.readString(),path)
            .filterIsInstance<Definition>()
            .distinctBy { it.identifier }
        val wirespec = KotlinEmitter(logger = noLogger).emit(ast)
        println(KotlinShared.source)
        println(wirespec.joinToString { it.result })
    }

    @Test
    fun testGlobalWeather() {
        val resource = Resource("src/commonTest/resources/global-weather.wsdl")
        val ast = WsdlParser.parseDefinitions(resource.readText(), base)
        println(ast)
    }

    @Test
    fun testCountryInfoService() {
        val resource = Resource("src/commonTest/resources/country-info-service.wsdl")
        val ast = WsdlParser.parseDefinitions(resource.readText(), base)
        val kotlin = KotlinEmitter(logger = noLogger).emit(ast)
        println(kotlin)
    }

    @Test
    fun testNumberConversion() {
        val resource = Resource("src/commonTest/resources/number-conversion.wsdl")
        val ast = WsdlParser.parseDefinitions(resource.readText(), base)
        println(ast)
    }
}