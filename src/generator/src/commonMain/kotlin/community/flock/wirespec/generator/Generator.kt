import community.flock.kotlinx.rgxgen.RgxGen
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.core.parse.Union
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.random.Random

object Generator {

    fun AST.generate(type: String, random: Random = Random.Default): JsonElement {
        val isIterable = type.endsWith("[]")
        val def = resolveDefinition(type.removeSuffix("[]"))
        return if (isIterable) {
            generateIterator(def, random)
        } else {
            generateObject(def, random)
        }
    }

    private fun AST.resolveDefinition(type: String) =
        this
            .filterIsInstance<Definition>()
            .find { it.name == type }
            ?: error("Definition not found in AST: $type")

    private fun AST.generateIterator(def: Definition, random: Random): JsonElement =
        (0..random.nextInt(10))
            .map { generateObject(def, random) }
            .let(::JsonArray)

    private fun AST.generateReference(ref: Reference, random: Random): JsonElement =
        when (ref) {
            is Reference.Primitive -> when (ref.type) {
                Reference.Primitive.Type.String -> RgxGen.parse("\\w{1,50}").generate(random).let(::JsonPrimitive)
                Reference.Primitive.Type.Integer -> random.nextInt().let(::JsonPrimitive)
                Reference.Primitive.Type.Number -> random.nextDouble().let(::JsonPrimitive)
                Reference.Primitive.Type.Boolean -> random.nextBoolean().let(::JsonPrimitive)
            }

            is Reference.Custom -> {
                val def = resolveDefinition(ref.value)
                if (ref.isIterable) {
                    generateIterator(def, random)
                } else {
                    generateObject(def, random)
                }

            }

            is Reference.Unit -> JsonNull
            is Reference.Any -> throw NotImplementedError("Cannot generate Any")
        }

    private fun AST.generateType(def: Type, random: Random): JsonObject {
        val typeSeed = random.nextInt()
        return def.shape.value
            .fold<Type.Shape.Field, Map<String, JsonElement>>(emptyMap()) { acc, cur ->
                cur.identifier.value.let { value ->
                    val fieldSeed = typeSeed + value.sumOf { it -> it.code }
                    val fieldRandom = Random(fieldSeed)
                    acc.plus(value to generateReference(cur.reference, fieldRandom))
                }
            }
            .let(::JsonObject)
    }

    private fun randomRegex(regex: String, random: Random): String {
        val rgxgen = RgxGen.parse(regex.substring(1, regex.length - 2))
        return rgxgen.generate(random)
    }

    private fun generateRefined(def: Refined, random: Random): JsonPrimitive {
        val regex = def.validator.value
        return randomRegex(regex, random).let(::JsonPrimitive)
    }

    private fun generateEnum(def: Enum, random: Random): JsonPrimitive {
        val index = random.nextInt(def.entries.size)
        return def.entries.toList()[index].let(::JsonPrimitive)
    }

    private fun AST.generateUnion(def: Union, random: Random): JsonElement {
        val index = random.nextInt(def.entries.size)
        val type = def.entries.toList()[index]
        return generate(type, random)
    }

    private fun AST.generateObject(def: Definition, random: Random): JsonElement {
        return when (def) {
            is Type -> generateType(def, random)
            is Refined -> generateRefined(def, random)
            is Enum -> generateEnum(def, random)
            is Union -> generateUnion(def, random)
            is Endpoint -> throw NotImplementedError("Endpoint cannot be generated")
        }
    }
}
