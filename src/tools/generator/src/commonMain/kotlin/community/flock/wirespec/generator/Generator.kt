package community.flock.wirespec.generator

import community.flock.kotlinx.rgxgen.RgxGen
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.random.Random

fun defaultGenerator(random: Random) = RgxGen.parse("\\w{1,50}").generate(random).let(::JsonPrimitive)
fun AST.generate(type: String, random: Random = Random.Default): JsonElement = Reference.Custom(
    value = type.removeSuffix("[]"),
    isNullable = false,
)
    .let { if (type.endsWith("[]")) Reference.Iterable(reference = it, isNullable = false) else it }
    .let { generate(it, random) }

fun AST.generate(type: Reference, random: Random = Random.Default): JsonElement = generateReference(type, random)

fun AST.generateRequest(endpointName: String, random: Random = Random.Default): JsonObject =
    generateRequest(resolveEndpoint(endpointName), random)

fun AST.generateRequest(endpoint: Endpoint, random: Random = Random.Default): JsonObject {
    val pathParams = endpoint.path
        .filterIsInstance<Endpoint.Segment.Param>()
        .associate { it.identifier.value to generateReference(it.reference, random) }
        .let(::JsonObject)

    val queries = endpoint.queries
        .associate { it.identifier.value to generateReference(it.reference, random) }
        .let(::JsonObject)

    val headers = endpoint.headers
        .associate { it.identifier.value to generateReference(it.reference, random) }
        .let(::JsonObject)

    val body = endpoint.requests.firstOrNull()?.content
        ?.let { generateReference(it.reference, random) }
        ?: JsonNull

    return JsonObject(
        mapOf(
            "path" to pathParams,
            "method" to JsonPrimitive(endpoint.method.name),
            "queries" to queries,
            "headers" to headers,
            "body" to body,
        ),
    )
}

fun AST.generateResponse(endpointName: String, random: Random = Random.Default): JsonObject =
    generateResponse(resolveEndpoint(endpointName), random)

fun AST.generateResponse(endpoint: Endpoint, random: Random = Random.Default): JsonObject =
    generateResponse(endpoint, endpoint.responses[random.nextInt(endpoint.responses.size)].status, random)

fun AST.generateResponse(endpointName: String, status: String, random: Random = Random.Default): JsonObject =
    generateResponse(resolveEndpoint(endpointName), status, random)

fun AST.generateResponse(endpoint: Endpoint, status: String, random: Random = Random.Default): JsonObject {
    val response = endpoint.responses.find { it.status == status }
        ?: error("Response with status $status not found in endpoint ${endpoint.identifier.value}")

    val headers = response.headers
        .associate { it.identifier.value to generateReference(it.reference, random) }
        .let(::JsonObject)

    val body = response.content
        ?.let { generateReference(it.reference, random) }
        ?: JsonNull

    return JsonObject(
        mapOf(
            "status" to JsonPrimitive(response.status.toInt()),
            "headers" to headers,
            "body" to body,
        ),
    )
}

private fun AST.resolveReference(type: Reference) = modules.flatMap { it.statements }.toList()
    .find { it.identifier.value == type.value }
    ?: error("Definition not found in AST: $type")

private fun AST.resolveEndpoint(name: String): Endpoint = modules.flatMap { it.statements }
    .filterIsInstance<Endpoint>()
    .find { it.identifier.value == name }
    ?: error("Endpoint not found in AST: $name")

private fun AST.generateIterator(def: Definition, random: Random): JsonElement = (0..random.nextInt(10))
    .map { generateObject(def, random) }
    .let(::JsonArray)

private fun AST.generateReference(ref: Reference, random: Random) = when (ref) {
    is Reference.Dict -> TODO()
    is Reference.Iterable -> generateIterator(resolveReference(ref.reference), random)
    is Reference.Primitive -> when (ref.type) {
        is Reference.Primitive.Type.Integer -> random.nextInt().let(::JsonPrimitive)
        is Reference.Primitive.Type.Number -> random.nextDouble().let(::JsonPrimitive)
        is Reference.Primitive.Type.Boolean -> random.nextBoolean().let(::JsonPrimitive)
        else -> defaultGenerator(random)
    }

    is Reference.Custom -> generateObject(resolveReference(ref), random)
    is Reference.Unit -> JsonNull
    is Reference.Any -> throw NotImplementedError("Cannot generate Any")
}

private fun AST.generateType(def: Type, random: Random): JsonObject = random.nextInt().let { typeSeed ->
    def.shape.value
        .fold<Field, Map<String, JsonElement>>(emptyMap()) { acc, cur ->
            cur.identifier.value.let { value ->
                val fieldSeed = typeSeed + value.sumOf { it.code }
                val fieldRandom = Random(fieldSeed)
                acc.plus(value to generateReference(cur.reference, fieldRandom))
            }
        }
        .let(::JsonObject)
}

private fun randomRegex(regex: String, random: Random) = RgxGen
    .parse(regex.substring(1, regex.length - 2))
    .generate(random)

private fun generateRefined(def: Refined, random: Random) = when (val type = def.reference.type) {
    is Reference.Primitive.Type.String -> when (val pattern = type.constraint) {
        is Reference.Primitive.Type.Constraint.RegExp -> randomRegex(pattern.value, random).let(::JsonPrimitive)
        null -> defaultGenerator(random)
    }
    Reference.Primitive.Type.Boolean -> random.nextBoolean().let(::JsonPrimitive)
    Reference.Primitive.Type.Bytes -> defaultGenerator(random)
    is Reference.Primitive.Type.Integer -> random.nextInt(
        from = type.constraint?.min?.toInt() ?: 0,
        until = type.constraint?.max?.toInt() ?: 0,
    ).let(::JsonPrimitive)

    is Reference.Primitive.Type.Number -> random.nextDouble(
        from = type.constraint?.min?.toDouble() ?: 0.0,
        until = type.constraint?.max?.toDouble() ?: 0.0,
    ).let(::JsonPrimitive)
}

private fun generateEnum(def: Enum, random: Random) = random
    .nextInt(def.entries.size)
    .let(def.entries.toList()::get)
    .let(::JsonPrimitive)

private fun AST.generateUnion(def: Union, random: Random) = random
    .nextInt(def.entries.size)
    .let(def.entries.toList()::get)
    .let { generate(it, random) }

private fun AST.generateObject(def: Definition, random: Random) = when (def) {
    is Type -> generateType(def, random)
    is Refined -> generateRefined(def, random)
    is Enum -> generateEnum(def, random)
    is Union -> generateUnion(def, random)
    is Endpoint -> throw NotImplementedError("Endpoint cannot be generated")
    is Channel -> throw NotImplementedError("Channel cannot be generated")
}
