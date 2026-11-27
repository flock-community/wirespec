package community.flock.wirespec.integration.jsonrpc

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import community.flock.wirespec.integration.jsonrpc.kotlinspec.generated.JsonRpcServer
import community.flock.wirespec.integration.jsonrpc.kotlinspec.generated.rpc.GetData
import community.flock.wirespec.integration.jsonrpc.kotlinspec.generated.rpc.NotifyHello
import community.flock.wirespec.integration.jsonrpc.kotlinspec.generated.rpc.NotifySum
import community.flock.wirespec.integration.jsonrpc.kotlinspec.generated.rpc.Subtract
import community.flock.wirespec.integration.jsonrpc.kotlinspec.generated.rpc.Sum
import community.flock.wirespec.integration.jsonrpc.kotlinspec.generated.rpc.Update
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.Optional
import java.util.concurrent.CompletableFuture
import community.flock.wirespec.integration.jackson.v2.java.WirespecSerialization as JavaSerialization
import community.flock.wirespec.integration.jackson.v2.kotlin.WirespecSerialization as KotlinSerialization
import community.flock.wirespec.integration.jsonrpc.javaspec.generated.JsonRpcServer as JavaServer
import community.flock.wirespec.integration.jsonrpc.javaspec.generated.rpc.GetData as JavaGetData
import community.flock.wirespec.integration.jsonrpc.javaspec.generated.rpc.NotifyHello as JavaNotifyHello
import community.flock.wirespec.integration.jsonrpc.javaspec.generated.rpc.NotifySum as JavaNotifySum
import community.flock.wirespec.integration.jsonrpc.javaspec.generated.rpc.Subtract as JavaSubtract
import community.flock.wirespec.integration.jsonrpc.javaspec.generated.rpc.Sum as JavaSum
import community.flock.wirespec.integration.jsonrpc.javaspec.generated.rpc.Update as JavaUpdate
import community.flock.wirespec.java.Wirespec as JavaWirespec

/**
 * Answers every example of section 7 of the JSON-RPC 2.0 specification, https://www.jsonrpc.org/specification, as the
 * specification does. `specification.json` holds the examples as they are written there: the request, and the response,
 * or null where the specification answers nothing.
 */
class SpecificationTest {

    private class Example(val example: String, val request: String, val response: String?)

    private val examples: List<Example> = jacksonObjectMapper().readValue(javaClass.getResource("/specification.json")!!)

    private val kotlin = JsonRpcServer(
        KotlinSerialization(jacksonObjectMapper()),
        object : Subtract.Service, Sum.Service, Update.Service, NotifyHello.Service, NotifySum.Service, GetData.Service {
            override suspend fun subtract(minuend: Long, subtrahend: Long): Long = minuend - subtrahend
            override suspend fun sum(a: Long, b: Long, c: Long): Long = a + b + c
            override suspend fun update(a: Long, b: Long, c: Long, d: Long, e: Long) = Unit
            override suspend fun notifyHello(value: Long) = Unit
            override suspend fun notifySum(a: Long, b: Long, c: Long) = Unit
            override suspend fun getData(): List<Any> = listOf("hello", 5)
        },
    )

    private interface JavaServices :
        JavaSubtract.Service,
        JavaSum.Service,
        JavaUpdate.Service,
        JavaNotifyHello.Service,
        JavaNotifySum.Service,
        JavaGetData.Service

    private val java = JavaServer(
        JavaSerialization(ObjectMapper().registerModule(Jdk8Module())),
        object : JavaServices {
            override fun subtract(minuend: Long, subtrahend: Long): CompletableFuture<Long> = CompletableFuture.completedFuture(minuend - subtrahend)
            override fun sum(a: Long, b: Long, c: Long): CompletableFuture<Long> = CompletableFuture.completedFuture(a + b + c)
            override fun update(a: Long, b: Long, c: Long, d: Long, e: Long): CompletableFuture<Void> = CompletableFuture.completedFuture(null)
            override fun notifyHello(value: Long): CompletableFuture<Void> = CompletableFuture.completedFuture(null)
            override fun notifySum(a: Long, b: Long, c: Long): CompletableFuture<Void> = CompletableFuture.completedFuture(null)
            override fun getData(): CompletableFuture<List<Any>> = CompletableFuture.completedFuture(listOf("hello", 5))
        },
    )

    @TestFactory
    fun `The Kotlin server answers the examples of the specification`(): List<DynamicTest> = examples.map { example ->
        DynamicTest.dynamicTest(example.example + ": " + example.request.lines().joinToString(" ") { it.trim() }) {
            runBlocking { kotlin.handle(Wirespec.RawRequest("POST", emptyList(), emptyMap(), emptyMap(), example.request.encodeToByteArray())) }
                .let { response -> example.shouldBeAnsweredWith(response.statusCode, response.body?.decodeToString()) }
        }
    }

    @TestFactory
    fun `The Java server answers the examples of the specification`(): List<DynamicTest> = examples.map { example ->
        DynamicTest.dynamicTest(example.example + ": " + example.request.lines().joinToString(" ") { it.trim() }) {
            java.handle(JavaWirespec.RawRequest("POST", emptyList(), emptyMap(), emptyMap(), Optional.of(example.request.toByteArray()))).join()
                .let { response -> example.shouldBeAnsweredWith(response.statusCode(), response.body().map { String(it) }.orElse(null)) }
        }
    }

    /** Where the specification answers nothing, the response has no body. */
    private fun Example.shouldBeAnsweredWith(status: Int, body: String?) = when (response) {
        null -> {
            status shouldBe 204
            body shouldBe null
        }
        else -> {
            status shouldBe 200
            body!! shouldEqualJson response
        }
    }
}
