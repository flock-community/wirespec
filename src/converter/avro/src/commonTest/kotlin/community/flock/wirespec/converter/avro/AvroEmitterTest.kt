package community.flock.wirespec.converter.avro

import com.goncalossilva.resources.Resource
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AvroEmitterTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test()
    fun testSimple() {

        val text = Resource("src/commonTest/resources/example.avsc")
            .apply { assertTrue(exists()) }
            .run { readText() }

        val model = json.decodeFromString<AvroModel.Type>(text)

        val ast = AvroParser.parse(text)
        val expected = json.encodeToString(model)
        val actual = AvroEmitter.emit(ast).let { json.encodeToString(it) }

//        assertEquals(expected, actual)
    }


}
