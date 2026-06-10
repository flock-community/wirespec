package community.flock.wirespec.integration.kotest

import community.flock.wirespec.scala.Wirespec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import scala.Option
import scala.collection.immutable.`List$`
import scala.collection.immutable.`Map$`
import scala.reflect.ClassTag
import scala.reflect.`ClassTag$`
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cover the Scala adapter against the Scala `Wirespec` runtime compiled into
 * `:src:integration:wirespec`, which stands in for the user's emit-shared
 * `Wirespec.scala`. Confirms:
 *   1. `kotestWirespecScalaGenerator(...)` returns something castable to the
 *      Scala `Wirespec.Generator` declared by the fixture.
 *   2. The reflective decoding routes a `GeneratorFieldString` through the
 *      commonMain algorithm.
 *   3. `GeneratorFieldNullable<T>` returns a `scala.Option<T>`, not a bare
 *      Kotlin `T?`.
 */
class KotestWirespecScalaGeneratorJvmTest {

    // Mimics a Scala-emitted Refined wrapper: single-arg primary ctor.
    data class FakeEmail(val value: String)

    data class FakeUser(val email: String)

    @Test
    fun `path override on a refined shape auto-wraps the drawn primitive`() {
        val gen = kotestWirespecScalaGenerator(seed = 0L) {
            registerPath("u", "email") { Arb.constant("a@b.com") }
        } as Wirespec.Generator

        // Scala-emitted shapes carry `scala.reflect.classTag[T]`; the adapter
        // must surface its runtimeClass so JvmRefinedWrapper can wrap the
        // drawn String. The callback must not run when the override fires.
        val emailShape = Wirespec.GeneratorFieldShape<FakeEmail>(
            emptyScalaMap(),
            { _ -> error("generate callback must not run when an override fires") },
            classTagOf(FakeEmail::class.java),
        )
        val v: Any? = gen.generate(scalaListOf("u", "email"), emailShape)
        assertEquals(FakeEmail("a@b.com"), v)
    }

    @Test
    fun `registerFieldByTypeName fires for Scala-emitted shapes carrying the parent ClassTag`() {
        val gen = kotestWirespecScalaGenerator(seed = 0L) {
            registerFieldByTypeName(FakeUser::class.qualifiedName!!, "email") { Arb.constant("a@b.com") }
        } as Wirespec.Generator

        val userShape = Wirespec.GeneratorFieldShape<FakeUser>(
            emptyScalaMap(),
            { p ->
                FakeUser(
                    gen.generate(
                        p.appended("email") as scala.collection.immutable.List<String>,
                        Wirespec.GeneratorFieldString(Option.empty(), emptyScalaList()),
                    ),
                )
            },
            classTagOf(FakeUser::class.java),
        )
        val v = gen.generate(scalaListOf("u"), userShape)
        assertEquals("a@b.com", v.email)
    }

    @Test
    fun `annotation decoding converts nested Scala HashMaps with 5 or more entries`() {
        // Scala immutable maps with <=4 entries are Map$.MapN instances; 5+
        // entries become scala.collection.immutable.HashMap. Both must decode
        // to Kotlin maps when nested inside an annotation.
        val nested = scalaMapOf("a" to 1, "b" to 2, "c" to 3, "d" to 4, "e" to 5)
        val annotation = scalaMapOf("name" to "X", "parameters" to nested)

        @Suppress("UNCHECKED_CAST")
        val field = Wirespec.GeneratorFieldString(
            Option.empty(),
            scalaListOfAny(annotation) as scala.collection.immutable.List<scala.collection.immutable.Map<String, Any>>,
        )
        val decoded = ScalaInterop.scalaFieldToKotest(field) as KotestFieldString
        val params = decoded.annotations.single()["parameters"]
        assertTrue(
            params is Map<*, *>,
            "expected nested Kotlin Map, got ${params?.let { it::class.qualifiedName }}",
        )
        assertEquals(5, (params as Map<*, *>).size)
    }

    @Test
    fun `factory returns a value assignable to the Scala Wirespec_Generator`() {
        val gen = kotestWirespecScalaGenerator(seed = 1L) as Wirespec.Generator
        assertNotNull(gen)
    }

    @Test
    fun `adapter wraps GeneratorFieldNullable result in scala_Option`() {
        val gen = kotestWirespecScalaGenerator(seed = 0L) as Wirespec.Generator

        val nullableField = Wirespec.GeneratorFieldNullable<String> { _ -> "value" }
        // The algorithm draws null for ~20% of paths; every draw must come
        // back as a scala.Option (never a bare T?/null), and defined draws
        // must carry the callback's value.
        val draws = (0 until 20).map { i ->
            val v: Option<String> = gen.generate(scalaListOf("n$i"), nullableField)
            v
        }
        assertTrue(draws.all { it != null }, "every draw must be a scala.Option")
        assertTrue(draws.any { it.isDefined }, "expected at least one defined draw")
        assertTrue(draws.filter { it.isDefined }.all { it.get() == "value" })
    }

    // --- helpers ---

    private fun scalaListOf(vararg xs: String): scala.collection.immutable.List<String> {
        var acc: scala.collection.immutable.List<String> = `List$`.`MODULE$`.empty()
        for (x in xs.reversed()) acc = acc.prepended(x)
        return acc
    }

    private fun <T> emptyScalaList(): scala.collection.immutable.List<T> = `List$`.`MODULE$`.empty()

    private fun scalaListOfAny(vararg xs: Any): scala.collection.immutable.List<Any> {
        var acc: scala.collection.immutable.List<Any> = `List$`.`MODULE$`.empty()
        for (x in xs.reversed()) acc = acc.prepended(x)
        return acc
    }

    @Suppress("UNCHECKED_CAST")
    private fun scalaMapOf(vararg pairs: Pair<String, Any>): scala.collection.immutable.Map<String, Any> {
        var tuples: scala.collection.immutable.List<scala.Tuple2<String, Any>> = `List$`.`MODULE$`.empty()
        for ((k, v) in pairs.reversed()) tuples = tuples.prepended(scala.Tuple2(k, v))
        return `Map$`.`MODULE$`.from(tuples) as scala.collection.immutable.Map<String, Any>
    }

    @Suppress("UNCHECKED_CAST")
    private fun <K, V> emptyScalaMap(): scala.collection.immutable.Map<K, V> = `Map$`.`MODULE$`.empty<K, V>() as scala.collection.immutable.Map<K, V>

    private fun <T> classTagOf(cls: Class<T>): ClassTag<T> = `ClassTag$`.`MODULE$`.apply(cls)
}
