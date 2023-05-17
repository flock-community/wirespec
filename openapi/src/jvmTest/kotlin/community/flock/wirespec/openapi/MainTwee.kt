@file:OptIn(ExperimentalStdlibApi::class)

package community.flock.wirespec.generated

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import community.flock.wirespec.generated.main.TodosIdGET
import kotlin.reflect.KType
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

data class Content<T> (val type:String, val body:T )
data class Request<T> ( val url:String, val method: String, val headers: Map<String, List<Any>>, val content:Content<T>? )
interface Response<T> { val status:Int; val headers: Map<String, List<Any>>; val content:Content<T> }
interface Api { fun handle(request: Request<*> ):Response<*> }
interface Mapper { fun <T> read(src: ByteArray, valueType: KType): T }

interface TodosList {
    sealed interface TodosListResponse
    sealed interface TodosListResponse200: TodosListResponse
    sealed interface TodosListResponse500: TodosListResponse
    data class Response200ApplicationJson(override val status: Int, override val headers: Map<String, List<Any>>, override val content: Content<List<Todo>> ): Response<List<Todo>>, TodosListResponse200
    data class Response500ApplicationJson(override val status: Int, override val headers: Map<String, List<Any>>, override val content: Content<Error> ): Response<Error>, TodosListResponse500
    suspend fun requestMapper(mapper: Mapper) =
        fun(status: Int,  contentType: String, headers:Map<String, List<String>>, src: ByteArray) =
            when ((status to contentType)) {
                (200 to "application/json") -> mapper
                    .read<List<Todo>>(src, typeOf<List<Todo>>())
                    .let { Response200ApplicationJson(status, headers, Content("application/json", it)) }
                (500 to "application/json") -> mapper
                    .read<Error>(src, typeOf<Error>())
                    .let { Response500ApplicationJson(status, headers, Content("application/json", it)) }

                else -> error("Cannot map")
            }
    suspend fun todosListUnit(completed: Boolean, xUser: Boolean ):TodosListResponse {
        Request<Unit>(
            url = "/todos",
            method = "GET",
            headers = mapOf("x-user" to listOf(xUser)),
            content = null,
        )
        TODO()
    }
    companion object{
        const val PATH = "/todos"
    }
}

interface TodosPOST {
    sealed interface TodosPOSTResponse
    sealed interface TodosPOSTResponse201: TodosPOSTResponse
    sealed interface TodosPOSTResponse500: TodosPOSTResponse
    data class Response201null(override val status: Int, override val headers: Map<String, List<Any>>, override val content: Content<Unit> ): Response<Unit>, TodosPOSTResponse201
    data class Response500ApplicationJson(override val status: Int, override val headers: Map<String, List<Any>>, override val content: Content<Error> ): Response<Error>, TodosPOSTResponse500
    suspend fun requestMapper(mapper: Mapper) =
        fun(status: Int,  contentType: String, headers:Map<String, List<String>>, src: ByteArray) =
            when ((status to contentType)) {
                (201 to "null") -> mapper
                    .read<Unit>(src, typeOf<Unit>())
                    .let { Response201null(status, headers, Content("null", it)) }
                (500 to "application/json") -> mapper
                    .read<Error>(src, typeOf<Error>())
                    .let { Response500ApplicationJson(status, headers, Content("application/json", it)) }

                else -> error("Cannot map")
            }
    suspend fun todosPOSTApplicationJson(xUser: Boolean , content: Todo_input):TodosPOSTResponse {
        Request<Todo_input>(
            url = "/todos",
            method = "POST",
            headers = mapOf("x-user" to listOf(xUser)),
            content = Content("application/json", content),
        )
        TODO()
    }
    suspend fun todosPOSTApplicationXml(xUser: Boolean , content: Todo):TodosPOSTResponse {
        Request<Todo>(
            url = "/todos",
            method = "POST",
            headers = mapOf("x-user" to listOf(xUser)),
            content = Content("application/xml", content),
        )
        TODO()
    }
    companion object{
        const val PATH = "/todos"
    }
}

interface TodosIdGET {
    sealed interface TodosIdGETResponse
    sealed interface TodosIdGETResponse200: TodosIdGETResponse
    sealed interface TodosIdGETResponse500: TodosIdGETResponse
    data class Response200ApplicationJson(override val status: Int, override val headers: Map<String, List<Any>>, override val content: Content<Todo> ): Response<Todo>, TodosIdGETResponse200
    data class Response500ApplicationJson(override val status: Int, override val headers: Map<String, List<Any>>, override val content: Content<Error> ): Response<Error>, TodosIdGETResponse500
    suspend fun requestMapper(mapper: Mapper) =
        fun(status: Int,  contentType: String, headers:Map<String, List<String>>, src: ByteArray) =
            when ((status to contentType)) {
                (200 to "application/json") -> mapper
                    .read<Todo>(src, typeOf<Todo>())
                    .let { Response200ApplicationJson(status, headers, Content("application/json", it)) }
                (500 to "application/json") -> mapper
                    .read<Error>(src, typeOf<Error>())
                    .let { Response500ApplicationJson(status, headers, Content("application/json", it)) }

                else -> error("Cannot map")
            }
    suspend fun todosIdGETUnit(id: String ):TodosIdGETResponse {
        Request<Unit>(
            url = "/todos/${id}",
            method = "GET",
            headers = mapOf(),
            content = null,
        )
        TODO()
    }
    companion object{
        const val PATH = "/todos/{id}"
    }
}

data class Todo_input(
    val title: String,
    val completed: Boolean
)

data class Todo(
    val id: String,
    val title: String,
    val completed: Boolean,
    val alert: TodoAlert
)

data class TodoAlert(
    val code: String,
    val message: TodoAlertMessage
)

data class TodoAlertMessage(
    val key: String,
    val value: String
)

data class TodosnestedArray(
    val id: String,
    val title: String,
    val nested: Boolean
)

data class Error(
    val code: String,
    val message: String
)




object Client : TodosIdGET {

    private val map = object : community.flock.wirespec.generated.main.Mapper {
        val objectMapper = ObjectMapper().registerKotlinModule()
        override fun <T> read(src: ByteArray, valueType: KType): T {
            val type = objectMapper.constructType(valueType.javaType)
            return objectMapper.readValue(src, type)
        }
    }

    override suspend fun <Req : community.flock.wirespec.generated.main.Request<*>, Res : community.flock.wirespec.generated.main.Response<*>> handle(
        request: Req,
        mapper: (community.flock.wirespec.generated.main.Mapper) -> (Int, String, Map<String, List<String>>, ByteArray) -> Res
    ): Res {
        val body = """{"code": "001", "message": "Hello World!"}""".toByteArray()
        return mapper(map)(500, "application/json", emptyMap(), body)
    }
}

suspend fun main() {
    val res = Client.todosIdGETUnit("123")
    val x = when (res) {
        is TodosIdGET.Response200ApplicationJson -> error("")
        is TodosIdGET.Response500ApplicationJson -> res.content.body.message
    }

    print(x)


}

