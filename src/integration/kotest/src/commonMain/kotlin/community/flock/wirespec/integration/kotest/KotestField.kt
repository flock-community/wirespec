package community.flock.wirespec.integration.kotest

import kotlin.reflect.KType

/**
 * Generation contract owned by the kotest integration, mirroring `Wirespec.Generator` but in
 * commonMain so the JS facade can implement it without the JVM-only `:src:integration:wirespec`
 * module. [kotestWirespecKotlinGenerator] adapts it to a `Wirespec.Generator` for IR-emitted callers.
 */
interface KotestGenerator {
    fun <T> generate(path: List<String>, field: KotestField<T>): T
}

/** Multiplatform 1:1 mirror of `Wirespec.GeneratorField<T>`. */
sealed interface KotestField<T>

/**
 * Leaf variants that carry Wirespec field annotations (e.g. `@Seed`). Container variants
 * (Array/Nullable/Dict) don't; Shape carries them per child field instead.
 */
sealed interface KotestLeafField<T> : KotestField<T> {
    val annotations: List<Map<String, Any>>
}

data class KotestFieldString(
    val regex: String?,
    override val annotations: List<Map<String, Any>>,
) : KotestLeafField<String>

data class KotestFieldInteger64(
    val min: Long?,
    val max: Long?,
    override val annotations: List<Map<String, Any>>,
) : KotestLeafField<Long>

data class KotestFieldInteger32(
    val min: Int?,
    val max: Int?,
    override val annotations: List<Map<String, Any>>,
) : KotestLeafField<Int>

data class KotestFieldNumber64(
    val min: Double?,
    val max: Double?,
    override val annotations: List<Map<String, Any>>,
) : KotestLeafField<Double>

data class KotestFieldNumber32(
    val min: Float?,
    val max: Float?,
    override val annotations: List<Map<String, Any>>,
) : KotestLeafField<Float>

data class KotestFieldBoolean(
    override val annotations: List<Map<String, Any>>,
) : KotestLeafField<Boolean>

data class KotestFieldBytes(
    override val annotations: List<Map<String, Any>>,
) : KotestLeafField<ByteArray>

data class KotestFieldEnum(
    val values: List<String>,
    override val annotations: List<Map<String, Any>>,
    val type: KType,
) : KotestLeafField<String>

data class KotestFieldUnion(
    val variants: List<String>,
    override val annotations: List<Map<String, Any>>,
    val type: KType,
) : KotestLeafField<String>

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
