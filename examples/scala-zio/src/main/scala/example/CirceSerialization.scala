package example

import community.flock.wirespec.scala.Wirespec
import community.flock.wirespec.generated.model.*
import io.circe.*
import io.circe.generic.semiauto.deriveCodec
import io.circe.parser.decode
import io.circe.syntax.*

import scala.reflect.ClassTag

object CirceSerialization extends Wirespec.Serialization {

  private given Codec.AsObject[MetricsDatasetsArray] = deriveCodec
  private given Codec.AsObject[ApiVersionInfo] = deriveCodec
  private given Codec.AsObject[ApiVersionExternalDocs] = deriveCodec
  private given Codec.AsObject[APIVersionsInfo] = deriveCodec
  private given Codec.AsObject[APIVersionsExternalDocs] = deriveCodec
  private given Codec.AsObject[MetricsThisWeek] = deriveCodec
  private given Codec.AsObject[ApiVersion] = deriveCodec
  private given Codec.AsObject[APIVersions] = deriveCodec
  private given Codec.AsObject[API] = deriveCodec
  private given Codec.AsObject[Metrics] = deriveCodec
  private given Codec.AsObject[GetProviders200ResponseBody] = deriveCodec
  private given Codec.AsObject[GetServices200ResponseBody] = deriveCodec

  private def codec[T: Encoder: Decoder: ClassTag]: (Class[?], (Encoder[?], Decoder[?])) =
    summon[ClassTag[T]].runtimeClass -> (summon[Encoder[T]], summon[Decoder[T]])

  private val codecRegistry: Map[Class[?], (Encoder[?], Decoder[?])] = Map(
    codec[Metrics],
    codec[API],
    codec[GetProviders200ResponseBody],
    codec[GetServices200ResponseBody],
    classOf[Map[?, ?]] -> (Encoder.encodeMap[String, API].asInstanceOf[Encoder[?]], Decoder.decodeMap[String, API].asInstanceOf[Decoder[?]]),
  )

  override def serializeBody[T](t: T, `type`: ClassTag[?]): Array[Byte] = {
    val cls = if (t.isInstanceOf[Map[?, ?]]) classOf[Map[?, ?]] else t.getClass
    val (encoder, _) = codecRegistry.getOrElse(cls, throw new IllegalStateException(s"No encoder for ${t.getClass}"))
    encoder.asInstanceOf[Encoder[T]].apply(t).noSpaces.getBytes("UTF-8")
  }

  override def deserializeBody[T](raw: Array[Byte], `type`: ClassTag[?]): T = {
    val cls = `type`.runtimeClass
    val key = if (classOf[Map[?, ?]].isAssignableFrom(cls)) classOf[Map[?, ?]] else cls
    val (_, decoder) = codecRegistry.getOrElse(key, throw new IllegalStateException(s"No decoder for $cls"))
    decode(new String(raw, "UTF-8"))(using decoder.asInstanceOf[Decoder[T]]).fold(throw _, identity)
  }

  override def serializePath[T](t: T, `type`: ClassTag[?]): String = t.toString

  override def deserializePath[T](raw: String, `type`: ClassTag[?]): T = {
    val cls = `type`.runtimeClass
    (if (cls == classOf[String]) raw
    else if (cls == classOf[java.lang.Long] || cls == classOf[Long]) raw.toLong
    else if (cls == classOf[java.lang.Integer] || cls == classOf[Int]) raw.toInt
    else if (cls == classOf[java.lang.Boolean] || cls == classOf[Boolean]) raw.toBoolean
    else if (cls == classOf[java.lang.Double] || cls == classOf[Double]) raw.toDouble
    else throw new IllegalStateException(s"Cannot deserialize path for $cls")).asInstanceOf[T]
  }

  override def serializeParam[T](value: T, `type`: ClassTag[?]): List[String] = List(value.toString)

  override def deserializeParam[T](values: List[String], `type`: ClassTag[?]): T =
    deserializePath(values.headOption.getOrElse(throw new IllegalStateException("Empty param list")), `type`)
}
