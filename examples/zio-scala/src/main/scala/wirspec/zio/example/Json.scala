package wirspec.zio.example

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import com.fasterxml.jackson.databind.module.SimpleModule
import community.flock.wirespec.generated.java.v3.PetStatus

object Json {


  private val petStatusSerializer = new JsonSerializer[PetStatus] {
    override def serialize(t: PetStatus, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit =
      jsonGenerator.writeString(t.label)
  }

  private val petStatusDeserializer = new JsonDeserializer[PetStatus] {
    override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): PetStatus = {
      deserializationContext.readValue(jsonParser, classOf[String]) match {
        case PetStatus.AVAILABLE.label => PetStatus.AVAILABLE
        case PetStatus.SOLD.label => PetStatus.SOLD
        case PetStatus.PENDING.label => PetStatus.PENDING
      }
    }
  }


  val jsonModule = new SimpleModule()
  jsonModule.addSerializer(classOf[PetStatus], petStatusSerializer)
  jsonModule.addDeserializer(classOf[PetStatus], petStatusDeserializer)

}
