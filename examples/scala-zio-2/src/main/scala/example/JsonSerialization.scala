package example

import community.flock.wirespec.scala.Wirespec
import community.flock.wirespec.generated.model.*
import zio.json.*
import zio.json.DeriveJsonCodec

import scala.reflect.ClassTag

object JsonSerialization extends Wirespec.Serialization {

  // Refined types encode/decode as their underlying primitive value
  private given JsonDecoder[TodoId]               = JsonDecoder.string.map(TodoId(_))
  private given JsonEncoder[TodoId]               = JsonEncoder.string.contramap(_.value)
  private given JsonDecoder[IntRefinedNoBound]    = JsonDecoder.long.map(IntRefinedNoBound(_))
  private given JsonEncoder[IntRefinedNoBound]    = JsonEncoder.long.contramap(_.value)
  private given JsonDecoder[IntRefinedLowerBound] = JsonDecoder.long.map(IntRefinedLowerBound(_))
  private given JsonEncoder[IntRefinedLowerBound] = JsonEncoder.long.contramap(_.value)
  private given JsonDecoder[IntRefinedLowerAndUpper] = JsonDecoder.long.map(IntRefinedLowerAndUpper(_))
  private given JsonEncoder[IntRefinedLowerAndUpper] = JsonEncoder.long.contramap(_.value)
  private given JsonDecoder[NumberRefinedNoBound] = JsonDecoder.double.map(NumberRefinedNoBound(_))
  private given JsonEncoder[NumberRefinedNoBound] = JsonEncoder.double.contramap(_.value)
  private given JsonDecoder[NumberRefinedUpperBound] = JsonDecoder.double.map(NumberRefinedUpperBound(_))
  private given JsonEncoder[NumberRefinedUpperBound] = JsonEncoder.double.contramap(_.value)
  private given JsonDecoder[NumberRefinedLowerAndUpper] = JsonDecoder.double.map(NumberRefinedLowerAndUpper(_))
  private given JsonEncoder[NumberRefinedLowerAndUpper] = JsonEncoder.double.contramap(_.value)

  private given JsonCodec[TodoDto]          = DeriveJsonCodec.gen
  private given JsonCodec[PotentialTodoDto] = DeriveJsonCodec.gen
  private given JsonCodec[TodoError]        = DeriveJsonCodec.gen
  private given JsonCodec[User]             = DeriveJsonCodec.gen
  private given JsonCodec[UserError]        = DeriveJsonCodec.gen

  private val encoders: Map[Class[?], JsonEncoder[?]] = Map(
    classOf[TodoDto]          -> summon[JsonEncoder[TodoDto]],
    classOf[PotentialTodoDto] -> summon[JsonEncoder[PotentialTodoDto]],
    classOf[TodoError]        -> summon[JsonEncoder[TodoError]],
    classOf[User]             -> summon[JsonEncoder[User]],
    classOf[UserError]        -> summon[JsonEncoder[UserError]],
  )

  private val listEncoders: Map[Class[?], JsonEncoder[?]] = Map(
    classOf[TodoDto] -> summon[JsonEncoder[List[TodoDto]]].asInstanceOf[JsonEncoder[?]],
    classOf[User]    -> summon[JsonEncoder[List[User]]].asInstanceOf[JsonEncoder[?]],
  )

  private val decoders: Map[Class[?], JsonDecoder[?]] = Map(
    classOf[TodoDto]          -> summon[JsonDecoder[TodoDto]],
    classOf[PotentialTodoDto] -> summon[JsonDecoder[PotentialTodoDto]],
    classOf[TodoError]        -> summon[JsonDecoder[TodoError]],
    classOf[User]             -> summon[JsonDecoder[User]],
    classOf[UserError]        -> summon[JsonDecoder[UserError]],
    classOf[List[?]]          -> summon[JsonDecoder[List[TodoDto]]].asInstanceOf[JsonDecoder[?]],
  )

  override def serializeBody[T](t: T, `type`: ClassTag[?]): Array[Byte] =
    t match
      case list: List[?] =>
        // Peek at the element type since ClassTag erases List's type parameter
        val elementClass = list.headOption.map(_.getClass).getOrElse(classOf[Nothing])
        val encoder = listEncoders.getOrElse(elementClass,
          if (list.isEmpty) return "[]".getBytes("UTF-8")
          else throw new IllegalStateException(s"No list encoder for element type $elementClass"))
        encoder.asInstanceOf[JsonEncoder[T]].encodeJson(t, None).toString.getBytes("UTF-8")
      case _ =>
        val encoder = encoders.getOrElse(t.getClass, throw new IllegalStateException(s"No encoder for ${t.getClass}"))
        encoder.asInstanceOf[JsonEncoder[T]].encodeJson(t, None).toString.getBytes("UTF-8")

  override def deserializeBody[T](raw: Array[Byte], `type`: ClassTag[?]): T = {
    val cls = `type`.runtimeClass
    val key = if (classOf[List[?]].isAssignableFrom(cls)) classOf[List[?]] else cls
    val decoder = decoders.getOrElse(key, throw new IllegalStateException(s"No decoder for $cls"))
    decoder.asInstanceOf[JsonDecoder[T]].decodeJson(new String(raw, "UTF-8"))
      .fold(msg => throw new IllegalStateException(s"JSON decode failed: $msg"), identity)
  }

  override def serializePath[T](t: T, `type`: ClassTag[?]): String = t.toString

  override def deserializePath[T](raw: String, `type`: ClassTag[?]): T =
    val cls = `type`.runtimeClass
    (if (cls == classOf[String]) raw
    else if (cls == classOf[java.lang.Long] || cls == classOf[Long]) raw.toLong
    else if (cls == classOf[java.lang.Integer] || cls == classOf[Int]) raw.toInt
    else if (cls == classOf[java.lang.Boolean] || cls == classOf[Boolean]) raw.toBoolean
    else if (cls == classOf[java.lang.Double] || cls == classOf[Double]) raw.toDouble
    else if (cls == classOf[TodoId]) TodoId(raw)
    else throw new IllegalStateException(s"Cannot deserialize path for $cls")).asInstanceOf[T]

  override def serializeParam[T](value: T, `type`: ClassTag[?]): List[String] = List(value.toString)

  override def deserializeParam[T](values: List[String], `type`: ClassTag[?]): T =
    deserializePath(values.headOption.getOrElse(throw new IllegalStateException("Empty param list")), `type`)
}
