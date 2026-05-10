package community.flock.wirespec.integration.kotest

import kotlin.reflect.KType

/**
 * Generation contract owned by the kotest integration. Mirrors the
 * `Wirespec.Generator` shape but lives in commonMain so the JS facade can
 * implement it without depending on the JVM-only `:src:integration:wirespec`
 * module. The JVM bridge in [kotestWirespecGenerator] adapts a
 * `KotestGenerator` into a `Wirespec.Generator` for IR-emitted callers.
 */
interface KotestGenerator {
    fun <T> generate(path: List<String>, field: KotestField<T>): T
}

/**
 * Multiplatform mirror of `Wirespec.GeneratorField<T>`. Each variant carries
 * the same fields as its Wirespec counterpart, so the JVM adapter can
 * translate the two sides 1:1 and the JS facade can construct them straight
 * from plain JS objects.
 */
sealed interface KotestField<T : Any?>

data class KotestFieldString(
    val regex: String?,
    val annotations: List<Map<String, Any>>,
) : KotestField<String>

data class KotestFieldInteger(
    val min: Long?,
    val max: Long?,
    val annotations: List<Map<String, Any>>,
) : KotestField<Long>

data class KotestFieldNumber(
    val min: Double?,
    val max: Double?,
    val annotations: List<Map<String, Any>>,
) : KotestField<Double>

data class KotestFieldBoolean(
    val annotations: List<Map<String, Any>>,
) : KotestField<Boolean>

data class KotestFieldBytes(
    val annotations: List<Map<String, Any>>,
) : KotestField<ByteArray>

data class KotestFieldEnum(
    val values: List<String>,
    val annotations: List<Map<String, Any>>,
    val type: KType,
) : KotestField<String>

data class KotestFieldUnion(
    val variants: List<String>,
    val annotations: List<Map<String, Any>>,
    val type: KType,
) : KotestField<String>

data class KotestFieldArray<T : Any>(
    val generate: (List<String>) -> T,
) : KotestField<List<T>>

data class KotestFieldNullable<T : Any>(
    val generate: (List<String>) -> T,
) : KotestField<T?>

data class KotestFieldShape<T : Any>(
    val annotations: Map<String, List<Map<String, Any>>>,
    val generate: (List<String>) -> T,
    val type: KType,
) : KotestField<T>

data class KotestFieldDict<V : Any>(
    val generate: (List<String>) -> V,
) : KotestField<Map<String, V>>
