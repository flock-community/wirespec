package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec

/**
 * JVM-facing factory: returns a `Wirespec.Generator`, which is the contract
 * IR-emitted `*Generator.generate(gen: Wirespec.Generator, …)` factories
 * expect. Internally builds a multiplatform [KotestGenerator] and wraps it
 * in [WirespecGeneratorAdapter], which translates `Wirespec.GeneratorField*`
 * inputs into the kotest-owned [KotestField] mirror types on each call.
 *
 * ```
 * val gen = kotestWirespecGenerator(seed = 1L) {
 *     register("orderId") { Arb.uuid().map(java.util.UUID::toString) }
 * }
 * val member = MemberGenerator.generate(gen, emptyList())
 * ```
 */
fun kotestWirespecGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): Wirespec.Generator = WirespecGeneratorAdapter(kotestGenerator(seed, block))

/**
 * Bridge between Wirespec's `Generator` / `GeneratorField*` (which live in
 * `:src:integration:wirespec`, JVM-only, Kotlin 1.9-pinned for downstream
 * binary compat) and kotest's commonMain `KotestGenerator` / `KotestField*`
 * mirror types. The two hierarchies are 1:1, so translation is a flat `when`
 * with no semantic logic.
 */
internal class WirespecGeneratorAdapter(private val inner: KotestGenerator) : Wirespec.Generator {

    @Suppress("UNCHECKED_CAST")
    override fun <T> generate(
        path: List<String>,
        field: Wirespec.GeneratorField<T>,
    ): T = inner.generate(path, field.toKotestField() as KotestField<T>) as T

    @Suppress("UNCHECKED_CAST")
    private fun Wirespec.GeneratorField<*>.toKotestField(): KotestField<*> = when (this) {
        is Wirespec.GeneratorFieldString -> KotestFieldString(regex, annotations)
        is Wirespec.GeneratorFieldInteger -> KotestFieldInteger(min, max, annotations)
        is Wirespec.GeneratorFieldNumber -> KotestFieldNumber(min, max, annotations)
        is Wirespec.GeneratorFieldBoolean -> KotestFieldBoolean(annotations)
        is Wirespec.GeneratorFieldBytes -> KotestFieldBytes(annotations)
        is Wirespec.GeneratorFieldEnum -> KotestFieldEnum(values, annotations, type)
        is Wirespec.GeneratorFieldUnion -> KotestFieldUnion(variants, annotations, type)
        is Wirespec.GeneratorFieldArray<*> -> {
            val gen = (this as Wirespec.GeneratorFieldArray<Any>).generate
            KotestFieldArray(gen)
        }
        is Wirespec.GeneratorFieldNullable<*> -> {
            val gen = (this as Wirespec.GeneratorFieldNullable<Any>).generate
            KotestFieldNullable(gen)
        }
        is Wirespec.GeneratorFieldShape<*> -> {
            val shape = this as Wirespec.GeneratorFieldShape<Any>
            KotestFieldShape(shape.annotations, shape.generate, shape.type)
        }
        is Wirespec.GeneratorFieldDict<*> -> {
            val gen = (this as Wirespec.GeneratorFieldDict<Any>).generate
            KotestFieldDict(gen)
        }
    }
}
