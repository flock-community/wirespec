package community.flock.wirespec.integration.kotest.generator

import kotlin.reflect.KType

internal interface KotestGenerator {
    fun <T> generate(path: List<String>, field: KotestField<T>): T
}

public sealed interface KotestField<T>

internal sealed interface KotestLeafField<T> : KotestField<T> {
    val annotations: List<Map<String, Any>>
}

internal data class KotestFieldString(
    val regex: String?,
    override val annotations: List<Map<String, Any>>,
) : KotestLeafField<String>

internal data class KotestFieldInteger64(
    val min: Long?,
    val max: Long?,
    override val annotations: List<Map<String, Any>>,
) : KotestLeafField<Long>

internal data class KotestFieldInteger32(
    val min: Int?,
    val max: Int?,
    override val annotations: List<Map<String, Any>>,
) : KotestLeafField<Int>

internal data class KotestFieldNumber64(
    val min: Double?,
    val max: Double?,
    override val annotations: List<Map<String, Any>>,
) : KotestLeafField<Double>

internal data class KotestFieldNumber32(
    val min: Float?,
    val max: Float?,
    override val annotations: List<Map<String, Any>>,
) : KotestLeafField<Float>

internal data class KotestFieldBoolean(
    override val annotations: List<Map<String, Any>>,
) : KotestLeafField<Boolean>

internal data class KotestFieldBytes(
    override val annotations: List<Map<String, Any>>,
) : KotestLeafField<ByteArray>

internal data class KotestFieldEnum(
    val values: List<String>,
    override val annotations: List<Map<String, Any>>,
) : KotestLeafField<String>

internal data class KotestFieldUnion(
    val variants: List<String>,
    override val annotations: List<Map<String, Any>>,
) : KotestLeafField<String>

internal data class KotestFieldArray<T : Any>(
    val generate: (List<String>) -> T,
) : KotestField<List<T>>

internal data class KotestFieldNullable<T : Any>(
    val generate: (List<String>) -> T,
) : KotestField<T?>

internal data class KotestFieldShape<T : Any>(
    val annotations: Map<String, List<Map<String, Any>>>,
    val generate: (List<String>) -> T,
    val type: KType,
) : KotestField<T>

internal data class KotestFieldDict<V : Any>(
    val generate: (List<String>) -> V,
) : KotestField<Map<String, V>>
