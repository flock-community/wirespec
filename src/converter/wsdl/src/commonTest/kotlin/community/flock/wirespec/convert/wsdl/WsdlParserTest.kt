package community.flock.wirespec.convert.wsdl

import com.goncalossilva.resources.Resource
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import kotlin.test.Test

class WsdlParserTest {

    @Test
    fun testGlobalWeather() {
        val resource = Resource("src/commonTest/resources/global-weather.wsdl")
        val ast = WsdlParser.parse(resource.readText())
        println(ast)
    }

    @Test
    fun testCountryInfoService() {
        val resource = Resource("src/commonTest/resources/country-info-service.wsdl")
        val ast = WsdlParser.parse(resource.readText())
        val kotlin = KotlinEmitter().emit(ast)
        println(kotlin)
    }

    @Test
    fun testNumberConversion() {
        val resource = Resource("src/commonTest/resources/number-conversion.wsdl")
        val ast = WsdlParser.parse(resource.readText())
        println(ast)
    }
}