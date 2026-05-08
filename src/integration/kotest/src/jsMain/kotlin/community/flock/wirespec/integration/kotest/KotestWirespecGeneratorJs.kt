@file:OptIn(ExperimentalJsExport::class)
@file:JsExport

package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map

/**
 * TS-callable entry point. The DSL/Arb-based [kotestWirespecGenerator] cannot
 * be `@JsExport`ed (lambda receivers + generic `Arb<T>` don't survive
 * Kotlin/JS export), so this thin facade adapts a plain
 * `Record<string, (seed: number) => string>` registry to the same underlying
 * `KotestWirespecGenerator` algorithm.
 *
 * From TypeScript:
 * ```ts
 * const gen = kotestWirespecGeneratorJs(1, { orderId: (s) => `ORD-${s}` })
 * ```
 */
fun kotestWirespecGeneratorJs(
    seed: Int,
    registrations: dynamic = null,
): Wirespec.Generator = kotestWirespecGenerator(seed.toLong()) {
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
