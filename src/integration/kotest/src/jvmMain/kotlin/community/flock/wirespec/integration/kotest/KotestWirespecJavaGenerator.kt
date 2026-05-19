package community.flock.wirespec.integration.kotest

import community.flock.wirespec.java.Wirespec
import java.util.Optional
import kotlin.reflect.typeOf

/**
 * JVM-facing factory for Java-emitted code: returns a `community.flock.wirespec
 * .java.Wirespec.Generator`, which is what Java IR-emitted `*Generator.generate(
 * Wirespec.Generator, …)` factories expect.
 *
 * ```
 * val gen: Wirespec.Generator = kotestWirespecJavaGenerator(seed = 1L) {
 *     register("orderId") { Arb.uuid().map(java.util.UUID::toString) }
 * }
 * val member = MemberGenerator.generate(gen, java.util.List.of())
 * ```
 *
 * Same DSL, default arb catalog, and `@Generator` / `@Seed` semantics as the
 * Kotlin sibling [kotestWirespecKotlinGenerator]. The difference is the
 * concrete `Wirespec.GeneratorField*` shape: Java records use
 * `java.util.Optional` for nullable min/max/regex and `java.util.function
 * .Function` for the `generate` callbacks; nullable fields return
 * `Optional<T>` rather than the bare `T?` the commonMain algorithm produces.
 */
fun kotestWirespecJavaGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): Wirespec.Generator = WirespecJavaGeneratorAdapter(kotestGenerator(seed, block = block))

/**
 * Bridge between the Java `Wirespec.GeneratorField*` records and the kotest
 * commonMain `KotestField*` mirror.
 *
 * Differences from the Kotlin adapter:
 * - `Optional<X>` ⇄ `X?` for `regex` / `min` / `max`.
 * - `java.util.function.Function<List<String>, T>` ⇄ `(List<String>) -> T`.
 * - `KotestField*` carries `kotlin.reflect.KType` for Enum/Union/Shape, but
 *   the kotest impl never reads it; pass `typeOf<Any>()` as a placeholder.
 * - For `GeneratorFieldNullable<T>`, wrap the inner `T?` result in
 *   `Optional.ofNullable(...)` on return.
 */
internal class WirespecJavaGeneratorAdapter(private val inner: KotestGenerator) : Wirespec.Generator {

    @Suppress("UNCHECKED_CAST")
    override fun <T> generate(
        path: List<String>,
        field: Wirespec.GeneratorField<T>,
    ): T = when (field) {
        is Wirespec.GeneratorFieldNullable<*> -> {
            val nullableField = field as Wirespec.GeneratorFieldNullable<Any>
            val value: Any? = inner.generate(
                path,
                KotestFieldNullable<Any> { p -> nullableField.generate.apply(p) },
            )
            Optional.ofNullable(value) as T
        }
        else -> inner.generate(path, field.toKotestField() as KotestField<T>) as T
    }

    @Suppress("UNCHECKED_CAST")
    private fun Wirespec.GeneratorField<*>.toKotestField(): KotestField<*> = when (this) {
        is Wirespec.GeneratorFieldString -> KotestFieldString(regex.orElse(null), annotations)
        is Wirespec.GeneratorFieldInteger64 -> KotestFieldInteger64(min.orElse(null), max.orElse(null), annotations)
        is Wirespec.GeneratorFieldInteger32 -> KotestFieldInteger32(min.orElse(null), max.orElse(null), annotations)
        is Wirespec.GeneratorFieldNumber64 -> KotestFieldNumber64(min.orElse(null), max.orElse(null), annotations)
        is Wirespec.GeneratorFieldNumber32 -> KotestFieldNumber32(min.orElse(null), max.orElse(null), annotations)
        is Wirespec.GeneratorFieldBoolean -> KotestFieldBoolean(annotations)
        is Wirespec.GeneratorFieldBytes -> KotestFieldBytes(annotations)
        is Wirespec.GeneratorFieldEnum -> KotestFieldEnum(values, annotations, typeOf<Any>())
        is Wirespec.GeneratorFieldUnion -> KotestFieldUnion(variants, annotations, typeOf<Any>())
        is Wirespec.GeneratorFieldArray<*> -> {
            val arr = this as Wirespec.GeneratorFieldArray<Any>
            KotestFieldArray<Any> { p -> arr.generate.apply(p) }
        }
        is Wirespec.GeneratorFieldNullable<*> ->
            error("GeneratorFieldNullable handled in generate(...) above")
        is Wirespec.GeneratorFieldShape<*> -> {
            val shape = this as Wirespec.GeneratorFieldShape<Any>
            KotestFieldShape<Any>(shape.annotations, { p -> shape.generate.apply(p) }, typeOf<Any>())
        }
        is Wirespec.GeneratorFieldDict<*> -> {
            val dict = this as Wirespec.GeneratorFieldDict<Any>
            KotestFieldDict<Any> { p -> dict.generate.apply(p) }
        }
    }
}
