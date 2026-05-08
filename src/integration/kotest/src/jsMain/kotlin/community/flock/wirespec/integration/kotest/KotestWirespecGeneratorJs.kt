@file:OptIn(ExperimentalJsExport::class)
@file:JsExport

package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import kotlin.reflect.typeOf

/**
 * `@JsExport`-friendly wrapper around an inner [Wirespec.Generator]. The wrapper
 * accepts JS plain-object `GeneratorField`s (identified by their `kind`
 * discriminator), translates them to Kotlin `Wirespec.GeneratorField*` instances,
 * delegates to the inner algorithm, and translates the result back to JS shapes.
 *
 * The non-generic `generate(path: Array<String>, field: dynamic): dynamic` signature
 * is what allows this method to survive Kotlin/JS production DCE — `@JsExport`
 * cannot surface the inner interface's generic
 * `<T>(path: List<String>, field: GeneratorField<T>): T`.
 *
 * From TypeScript:
 * ```ts
 * const gen = kotestWirespecGeneratorJs(1, { orderId: (s) => `ORD-${s}` })
 * const value = gen.generate(["path"], { kind: "string", regex: undefined, annotations: [] })
 * ```
 */
class WirespecGeneratorJs internal constructor(private val inner: Wirespec.Generator) {

    fun generate(path: Array<String>, field: dynamic): dynamic {
        val kotlinField = jsToKotlinField(field)
        @Suppress("UNCHECKED_CAST")
        val result = inner.generate(path.toList(), kotlinField as Wirespec.GeneratorField<Any?>)
        return kotlinToJs(result)
    }
}

fun kotestWirespecGeneratorJs(
    seed: Int,
    registrations: dynamic = null,
): WirespecGeneratorJs {
    val inner = kotestWirespecGenerator(seed.toLong()) {
        if (registrations != null) {
            val keys = js("Object").keys(registrations) as Array<String>
            for (key in keys) {
                val factory = registrations[key].unsafeCast<(Int) -> String>()
                register(key) {
                    Arb.int(0..Int.MAX_VALUE).map { factory(it) }
                }
            }
        }
    }
    return WirespecGeneratorJs(inner)
}

private fun jsToKotlinField(field: dynamic): Wirespec.GeneratorField<*> {
    val kind = field.kind as String
    return when (kind) {
        "string" -> Wirespec.GeneratorFieldString(
            regex = field.regex as String?,
            annotations = jsAnnotationsToKotlin(field.annotations),
        )
        "integer" -> Wirespec.GeneratorFieldInteger(
            min = (field.min as? Number)?.toLong(),
            max = (field.max as? Number)?.toLong(),
            annotations = jsAnnotationsToKotlin(field.annotations),
        )
        "number" -> Wirespec.GeneratorFieldNumber(
            min = (field.min as? Number)?.toDouble(),
            max = (field.max as? Number)?.toDouble(),
            annotations = jsAnnotationsToKotlin(field.annotations),
        )
        "boolean" -> Wirespec.GeneratorFieldBoolean(jsAnnotationsToKotlin(field.annotations))
        "bytes" -> Wirespec.GeneratorFieldBytes(jsAnnotationsToKotlin(field.annotations))
        "enum" -> Wirespec.GeneratorFieldEnum(
            values = (field.values.unsafeCast<Array<String>>()).toList(),
            annotations = jsAnnotationsToKotlin(field.annotations),
            type = typeOf<String>(),
        )
        "union" -> Wirespec.GeneratorFieldUnion(
            variants = (field.variants.unsafeCast<Array<String>>()).toList(),
            annotations = jsAnnotationsToKotlin(field.annotations),
            type = typeOf<String>(),
        )
        "array" -> {
            val jsGen = field.generate.unsafeCast<(Array<String>) -> dynamic>()
            Wirespec.GeneratorFieldArray<Any> { p ->
                jsGen(p.toTypedArray()) as? Any ?: error("array element callback returned null at path $p")
            }
        }
        "nullable" -> {
            val jsGen = field.generate.unsafeCast<(Array<String>) -> dynamic>()
            Wirespec.GeneratorFieldNullable<Any> { p ->
                jsGen(p.toTypedArray()) as? Any ?: error("nullable inner callback returned null at path $p")
            }
        }
        "shape" -> {
            val jsGen = field.generate.unsafeCast<(Array<String>) -> dynamic>()
            Wirespec.GeneratorFieldShape<Any>(
                annotations = jsShapeAnnotationsToKotlin(field.annotations),
                generate = { p ->
                    jsGen(p.toTypedArray()) as? Any ?: error("shape callback returned null at path $p")
                },
                type = typeOf<Any>(),
            )
        }
        "dict" -> {
            val jsGen = field.generate.unsafeCast<(Array<String>) -> dynamic>()
            Wirespec.GeneratorFieldDict<Any> { p ->
                jsGen(p.toTypedArray()) as? Any ?: error("dict value callback returned null at path $p")
            }
        }
        else -> error("Unknown GeneratorField kind: '$kind'")
    }
}

private fun jsAnnotationsToKotlin(jsAnns: dynamic): List<Map<String, Any>> {
    if (jsAnns == null) return emptyList()
    val arr = jsAnns.unsafeCast<Array<dynamic>>()
    return arr.map { jsObjToKotlinMap(it) }
}

private fun jsShapeAnnotationsToKotlin(jsAnns: dynamic): Map<String, List<Map<String, Any>>> {
    if (jsAnns == null) return emptyMap()
    val keys = js("Object").keys(jsAnns) as Array<String>
    return keys.associateWith { k -> jsAnnotationsToKotlin(jsAnns[k]) }
}

private fun jsObjToKotlinMap(obj: dynamic): Map<String, Any> {
    val keys = js("Object").keys(obj) as Array<String>
    return keys.associateWith { k -> jsValueToAny(obj[k]) }
}

private fun jsValueToAny(v: dynamic): Any {
    val type = js("typeof v") as String
    return when (type) {
        "object" -> when {
            v == null -> error("Unexpected null in annotation value")
            js("Array.isArray(v)") as Boolean -> (v.unsafeCast<Array<dynamic>>()).map { jsValueToAny(it) }
            else -> jsObjToKotlinMap(v)
        }
        else -> v as Any
    }
}

private fun kotlinToJs(value: Any?): dynamic {
    if (value == null) return null
    return when (value) {
        is Long -> value.toDouble()
        is List<*> -> {
            val arr: dynamic = js("[]")
            for (item in value) arr.push(kotlinToJs(item))
            arr
        }
        is Map<*, *> -> {
            val obj: dynamic = js("({})")
            for ((k, v) in value) obj[k as String] = kotlinToJs(v)
            obj
        }
        else -> value
    }
}
