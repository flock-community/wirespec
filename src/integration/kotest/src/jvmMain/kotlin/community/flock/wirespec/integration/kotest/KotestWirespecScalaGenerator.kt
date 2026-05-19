package community.flock.wirespec.integration.kotest

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * JVM-facing factory for Scala-emitted code. Returns a value assignable to
 * `community.flock.wirespec.scala.Wirespec.Generator` at the call site:
 *
 * ```
 * val gen = kotestWirespecScalaGenerator(seed = 1L)
 *     as community.flock.wirespec.scala.Wirespec.Generator
 * val member = MemberGenerator.generate(gen, scala.collection.immutable.List.empty())
 * ```
 *
 * Unlike the Kotlin and Java siblings, this factory has **zero compile-time
 * dependency on Scala types** — the kotest module ships no Scala. The Scala
 * `Wirespec` interface is resolved at construction from the runtime classpath,
 * which the user populates by running their codegen with `--emit-shared` so
 * the generated `Wirespec.scala` lands on the test classpath.
 *
 * If the Scala-emitted `Wirespec` is missing, construction raises
 * `IllegalStateException` pointing at the cause.
 */
fun kotestWirespecScalaGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): Any = WirespecScalaGeneratorAdapter.create(kotestGenerator(seed, block = block))

/**
 * Eager-proxy Scala adapter. The classpath lookup is performed once at factory
 * time so missing-runtime errors surface immediately rather than mid-test.
 */
internal object WirespecScalaGeneratorAdapter {

    fun create(inner: KotestGenerator): Any {
        val cl = Thread.currentThread().contextClassLoader
            ?: javaClass.classLoader
        val generatorIface = try {
            cl.loadClass("community.flock.wirespec.scala.Wirespec\$Generator")
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException(
                "Scala-emitted Wirespec.scala not found on classpath. " +
                    "Run your codegen with --emit-shared and make sure the " +
                    "generated source set is on the test compile/runtime classpath.",
                e,
            )
        }
        val handler = InvocationHandler { _, method, args ->
            ScalaInterop.dispatch(inner, method, args ?: emptyArray())
        }
        // Create the proxy with the loader that actually defined the interface,
        // falling back to `cl` only if the interface was loaded by the bootstrap
        // classloader (where `classLoader` returns null). This avoids
        // IllegalArgumentException in environments where `cl` and the
        // interface's loader differ (shaded jars, custom test runners).
        val proxyLoader = generatorIface.classLoader ?: cl
        return Proxy.newProxyInstance(proxyLoader, arrayOf(generatorIface), handler)
    }
}
