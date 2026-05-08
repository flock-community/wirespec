package community.flock.wirespec.integration.kotest

import io.kotest.property.Arb

/**
 * No JS-side extras: `kotest-property-arbs` only publishes a legacy-JS-compiler
 * artifact, which is incompatible with the IR backend used here. JS users get
 * the core `email` / `uuid` / `ipAddress` defaults from [DEFAULT_ARBS]; anything
 * else can be added per-instance via the builder's `register(...)` DSL.
 */
internal actual val ARBS_EXTRAS: Map<String, () -> Arb<String>> = emptyMap()
