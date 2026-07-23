package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec

/**
 * JVM-facing factory for Kotlin-emitted code: returns the `Wirespec.Generator` that IR-emitted
 * `*Generator.generate(...)` factories expect, wrapping a multiplatform [KotestGenerator].
 *
 * ```
 * val gen = kotestWirespecKotlinGenerator(seed = 1L) {
 *     registerField(Member::id) { Arb.uuid().map(java.util.UUID::toString) }
 *     registerPath("users", "*", "email") { Arb.email() }
 * }
 * val member = MemberGenerator.generate(gen, emptyList())
 * ```
 */
fun kotestWirespecKotlinGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): Wirespec.Generator = WirespecKotlinGeneratorAdapter(
    kotestGenerator(seed, refinedWrapper = JvmRefinedWrapper, block = block),
)

/**
 * Bridges Wirespec's JVM-only Kotlin `Generator` / `GeneratorField*` and kotest's commonMain
 * `KotestGenerator` / `KotestField*` mirror types. The hierarchies are 1:1, so this is a flat `when`.
 */
internal class WirespecKotlinGeneratorAdapter(private val inner: KotestGenerator) : Wirespec.Generator {

    @Suppress("UNCHECKED_CAST")
    override fun <T> generate(
        path: List<String>,
        field: Wirespec.GeneratorField<T>,
    ): T = inner.generate(path, field.toKotestField() as KotestField<T>) as T

    @Suppress("UNCHECKED_CAST")
    private fun Wirespec.GeneratorField<*>.toKotestField(): KotestField<*> = when (this) {
        is Wirespec.GeneratorFieldString -> KotestFieldString(regex, annotations)
        is Wirespec.GeneratorFieldInteger64 -> KotestFieldInteger64(min, max, annotations)
        is Wirespec.GeneratorFieldInteger32 -> KotestFieldInteger32(min, max, annotations)
        is Wirespec.GeneratorFieldNumber64 -> KotestFieldNumber64(min, max, annotations)
        is Wirespec.GeneratorFieldNumber32 -> KotestFieldNumber32(min, max, annotations)
        is Wirespec.GeneratorFieldBoolean -> KotestFieldBoolean(annotations)
        is Wirespec.GeneratorFieldBytes -> KotestFieldBytes(annotations)
        is Wirespec.GeneratorFieldEnum -> KotestFieldEnum(values, annotations)
        is Wirespec.GeneratorFieldUnion -> KotestFieldUnion(variants, annotations)
        is Wirespec.GeneratorFieldArray<*> -> KotestFieldArray((this as Wirespec.GeneratorFieldArray<Any>).generate)
        is Wirespec.GeneratorFieldNullable<*> -> KotestFieldNullable((this as Wirespec.GeneratorFieldNullable<Any>).generate)
        is Wirespec.GeneratorFieldShape<*> -> (this as Wirespec.GeneratorFieldShape<Any>).let { KotestFieldShape(it.annotations, it.generate, it.type) }
        is Wirespec.GeneratorFieldDict<*> -> KotestFieldDict((this as Wirespec.GeneratorFieldDict<Any>).generate)
    }
}
