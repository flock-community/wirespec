package example

import community.flock.wirespec.scala.Wirespec
import community.flock.wirespec.generated.model.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.parser.decode
import io.circe.syntax.*

import scala.reflect.ClassTag

object CirceSerialization extends Wirespec.Serialization {

  // Codecs for empty case classes (schemaless / free-form types)
  private given Encoder[ApiVersionInfo] = Encoder.instance(_ => Json.obj())
  private given Decoder[ApiVersionInfo] = Decoder.instance(_ => Right(ApiVersionInfo()))
  private given Encoder[ApiVersionExternalDocs] = Encoder.instance(_ => Json.obj())
  private given Decoder[ApiVersionExternalDocs] = Decoder.instance(_ => Right(ApiVersionExternalDocs()))
  private given Encoder[APIVersionsInfo] = Encoder.instance(_ => Json.obj())
  private given Decoder[APIVersionsInfo] = Decoder.instance(_ => Right(APIVersionsInfo()))
  private given Encoder[APIVersionsExternalDocs] = Encoder.instance(_ => Json.obj())
  private given Decoder[APIVersionsExternalDocs] = Decoder.instance(_ => Right(APIVersionsExternalDocs()))
  private given Encoder[MetricsDatasetsArray] = Encoder.instance(_ => Json.obj())
  private given Decoder[MetricsDatasetsArray] = Decoder.instance(_ => Right(MetricsDatasetsArray()))

  // Codecs for case classes
  private given Encoder[MetricsThisWeek] = deriveEncoder[MetricsThisWeek]
  private given Decoder[MetricsThisWeek] = deriveDecoder[MetricsThisWeek]
  private given Encoder[ApiVersion] = deriveEncoder[ApiVersion]
  private given Decoder[ApiVersion] = deriveDecoder[ApiVersion]
  private given Encoder[APIVersions] = deriveEncoder[APIVersions]
  private given Decoder[APIVersions] = deriveDecoder[APIVersions]
  private given Encoder[API] = deriveEncoder[API]
  private given Decoder[API] = deriveDecoder[API]
  private given Encoder[Metrics] = deriveEncoder[Metrics]
  private given Decoder[Metrics] = deriveDecoder[Metrics]
  private given Encoder[GetProviders200ResponseBody] = deriveEncoder[GetProviders200ResponseBody]
  private given Decoder[GetProviders200ResponseBody] = deriveDecoder[GetProviders200ResponseBody]
  private given Encoder[GetServices200ResponseBody] = deriveEncoder[GetServices200ResponseBody]
  private given Decoder[GetServices200ResponseBody] = deriveDecoder[GetServices200ResponseBody]

  // Codec registries keyed by runtime class
  private val encoderRegistry: Map[Class[?], Encoder[?]] = Map(
    classOf[Metrics] -> Encoder[Metrics],
    classOf[MetricsThisWeek] -> Encoder[MetricsThisWeek],
    classOf[API] -> Encoder[API],
    classOf[ApiVersion] -> Encoder[ApiVersion],
    classOf[APIVersions] -> Encoder[APIVersions],
    classOf[GetProviders200ResponseBody] -> Encoder[GetProviders200ResponseBody],
    classOf[GetServices200ResponseBody] -> Encoder[GetServices200ResponseBody],
  )

  private val decoderRegistry: Map[Class[?], Decoder[?]] = Map(
    classOf[Metrics] -> Decoder[Metrics],
    classOf[MetricsThisWeek] -> Decoder[MetricsThisWeek],
    classOf[API] -> Decoder[API],
    classOf[ApiVersion] -> Decoder[ApiVersion],
    classOf[APIVersions] -> Decoder[APIVersions],
    classOf[GetProviders200ResponseBody] -> Decoder[GetProviders200ResponseBody],
    classOf[GetServices200ResponseBody] -> Decoder[GetServices200ResponseBody],
  )

  override def serializeBody[T](t: T, `type`: ClassTag[?]): Array[Byte] = {
    val json = t match {
      case v: scala.collection.immutable.Map[?, ?] =>
        // Map[String, API] used by GetProvider, ListAPIs, etc.
        given Encoder[Map[String, API]] = Encoder.encodeMap[String, API]
        v.asInstanceOf[Map[String, API]].asJson
      case _ =>
        val encoder = encoderRegistry.getOrElse(
          t.getClass,
          throw new IllegalStateException(s"No encoder for ${t.getClass}")
        ).asInstanceOf[Encoder[T]]
        encoder(t)
    }
    json.noSpaces.getBytes("UTF-8")
  }

  override def deserializeBody[T](raw: Array[Byte], `type`: ClassTag[?]): T = {
    val jsonStr = new String(raw, "UTF-8")
    val cls = `type`.runtimeClass
    val result: Any =
      if (classOf[Map[?, ?]].isAssignableFrom(cls)) {
        // Map[String, API] used by GetProvider, ListAPIs, etc.
        given Decoder[Map[String, API]] = Decoder.decodeMap[String, API]
        decode[Map[String, API]](jsonStr).fold(throw _, identity)
      } else {
        val decoder = decoderRegistry.getOrElse(
          cls,
          throw new IllegalStateException(s"No decoder for $cls")
        ).asInstanceOf[Decoder[Any]]
        decode(jsonStr)(using decoder).fold(throw _, identity)
      }
    result.asInstanceOf[T]
  }

  override def serializePath[T](t: T, `type`: ClassTag[?]): String =
    t.toString

  override def deserializePath[T](raw: String, `type`: ClassTag[?]): T = {
    val cls = `type`.runtimeClass
    val result: Any =
      if (cls == classOf[String]) raw
      else if (cls == classOf[java.lang.Long] || cls == classOf[Long]) raw.toLong
      else if (cls == classOf[java.lang.Integer] || cls == classOf[Int]) raw.toInt
      else if (cls == classOf[java.lang.Boolean] || cls == classOf[Boolean]) raw.toBoolean
      else if (cls == classOf[java.lang.Double] || cls == classOf[Double]) raw.toDouble
      else throw new IllegalStateException(s"Cannot deserialize path for $cls")
    result.asInstanceOf[T]
  }

  override def serializeParam[T](value: T, `type`: ClassTag[?]): List[String] =
    List(value.toString)

  override def deserializeParam[T](values: List[String], `type`: ClassTag[?]): T = {
    val raw = values.headOption.getOrElse(throw new IllegalStateException("Empty param list"))
    deserializePath(raw, `type`)
  }
}
