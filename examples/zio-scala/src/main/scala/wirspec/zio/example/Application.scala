package wirspec.zio.example

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import community.flock.wirespec.Wirespec
import community.flock.wirespec.generated.java.v3.{AddPet, Pet, PetStatus, UpdatePet}
import zio._
import zio.http._

import java.lang.reflect.Type
import scala.language.higherKinds

trait PetStoreClient[F[_]] extends AddPet[F] with UpdatePet[F]

object Main extends ZIOAppDefault {

  private val contentMapper: Wirespec.ContentMapper[String] = new Wirespec.ContentMapper[String] {
    val objectMapper: ObjectMapper = new ObjectMapper()
      .registerModule(DefaultScalaModule)
      .registerModule(Json.jsonModule)

    override def read[T](content: Wirespec.Content[String], valueType: Type): Wirespec.Content[T] = {
      val `type` = objectMapper.constructType(valueType)
      Wirespec.Content[T](content.`type`, objectMapper.readValue(content.body, `type`))
    }

    override def write[T](content: Wirespec.Content[T]): Wirespec.Content[String] = {
      content.copy(body = objectMapper.writeValueAsString(content.body))
    }
  }

  private def handle[Req <: Wirespec.Request[_], Res <: Wirespec.Response[_]](request: Req, mapper: (Wirespec.ContentMapper[String], Int, Map[String, List[Any]], Wirespec.Content[String]) => Res): ZIO[Client, Throwable, Res] = {
    val url = "https://petstore3.swagger.io/api/v3"
    val method = Method.fromString(request.method.label)
    val content = contentMapper.write(request.content)
    val body = Body.fromString(content.body)
    val headers = Headers(Header.ContentType(MediaType.forContentType(content.`type`).orNull))
    val client = for {
      res <- Client.request(url + request.path, method, headers, body)
      data <- res.body.asString
      headers = res.headers.map(x => x.headerName -> List(x.renderedValue)).toMap
      content = res.headers.get("Content-Type").map(`type` => Wirespec.Content(`type`, data)).orNull
    } yield mapper(contentMapper, res.status.code, headers, content)
    client
  }

  private val client = new PetStoreClient[Task]() {
    override def addPet(request: AddPet.Request[_]): Task[AddPet.Response[_]] = handle(request, AddPet.RESPONSE_MAPPER[String]).provide(Client.default)
    override def updatePet(request: UpdatePet.Request[_]): Task[UpdatePet.Response[_]] = handle(request, UpdatePet.RESPONSE_MAPPER[String]).provide(Client.default)
  }

  override val run = {
    val random = new scala.util.Random()
    val pet = Pet(
      id = Option.apply(random.nextInt(1000000)),
      name = "Test Diertje",
      photoUrls = List(),
      category = Option.empty,
      tags = Option.empty,
      status = Option.apply(PetStatus.AVAILABLE)
    )
    val req = new AddPet.RequestApplicationJson(pet)

    val res: ZIO[Any, Throwable, Pet] = client.addPet(req).map {
      case req: AddPet.Response200ApplicationJson => req.content.body
      case req: AddPet.Response200ApplicationXml => throw new IllegalArgumentException()
      case req: AddPet.Response405Unit => throw new IllegalArgumentException()
    }

    res.map(x => println(x))

  }
}
