package community.flock.wirespec.integration.kotest.generator

import community.flock.wirespec.kotlin.Wirespec

/** JVM-facing factory returning the `Wirespec.Generator` that IR-emitted code expects, wrapping a [KotestGenerator]. */
fun kotestWirespecKotlinGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): Wirespec.Generator = WirespecKotlinGeneratorAdapter(
    kotestGenerator(seed, refinedWrapper = JvmRefinedWrapper, block = block),
)

/** Bridges Wirespec's JVM-only Kotlin `Generator`/`GeneratorField*` to kotest's commonMain mirror types. */
internal class WirespecKotlinGeneratorAdapter(private val inner: KotestGenerator) : Wirespec.Generator {

    @Suppress("UNCHECKED_CAST")
    override fun <T> generate(
        path: List<String>,
        field: Wirespec.GeneratorField<T>,
    ): T = inner.generate(path, field.toKotestField() as KotestField<T>) as T

    @Suppress("UNCHECKED_CAST")
    private fun Wirespec.GeneratorField<*>.toKotestField(): KotestField<*> = when (val field = this) {
        is Wirespec.GeneratorFieldString -> KotestFieldString(regex, annotations)
        is Wirespec.GeneratorFieldInteger64 -> KotestFieldInteger64(min, max, annotations)
        is Wirespec.GeneratorFieldInteger32 -> KotestFieldInteger32(min, max, annotations)
        is Wirespec.GeneratorFieldNumber64 -> KotestFieldNumber64(min, max, annotations)
        is Wirespec.GeneratorFieldNumber32 -> KotestFieldNumber32(min, max, annotations)
        is Wirespec.GeneratorFieldBoolean -> KotestFieldBoolean(annotations)
        is Wirespec.GeneratorFieldBytes -> KotestFieldBytes(annotations)
        is Wirespec.GeneratorFieldEnum -> KotestFieldEnum(values, annotations)
        is Wirespec.GeneratorFieldUnion -> KotestFieldUnion(variants, annotations)
        is Wirespec.GeneratorFieldArray<*> -> KotestFieldArray(field.generate)
        is Wirespec.GeneratorFieldNullable<*> -> KotestFieldNullable(field.generate)
        is Wirespec.GeneratorFieldShape<*> -> KotestFieldShape(annotations, generate, type)
        is Wirespec.GeneratorFieldDict<*> -> KotestFieldDict(field.generate)
    }
}
