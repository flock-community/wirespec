package community.flock.wirespec.integration.kotest

import io.kotest.property.Arb
import io.kotest.property.arbitrary.email
import io.kotest.property.arbitrary.ipAddressV4

/**
 * Default catalog of `@Generator("name")` registrations preinstalled on every
 * `kotestWirespecGenerator` instance. Keys mirror the source `Arb.xxx()` names
 * for readability; lookup is case-insensitive (the registry lowercases keys on
 * `register`), so `@Generator("firstName")`, `@Generator("firstname")`, and
 * `@Generator("FIRSTNAME")` all resolve to the same entry.
 *
 * Source libraries:
 * - **kotest-property** (core, all platforms): `email`, `ipAddress`.
 * - JVM-only extras (see [ARBS_EXTRAS]): `uuid` (uses `java.util.UUID`)
 *   plus the `kotest-property-arbs` catalog: `firstName`, `lastName`,
 *   `fullName` (= `name`), `username`, `domain`, `color`.
 *
 * Override an entry with `register("name") { … }` in the builder block.
 */
internal val DEFAULT_ARBS: Map<String, () -> Arb<String>> = mapOf(
    "email" to { Arb.email() },
    "ipAddress" to { Arb.ipAddressV4() },

) + ARBS_EXTRAS

/**
 * Platform-specific default registrations. JVM provides `Arb.uuid` plus the
 * `kotest-property-arbs` extras; JS leaves it empty (`kotest-property-arbs`
 * doesn't publish an IR-compatible artifact, and `Arb.uuid` requires
 * `java.util.UUID`).
 */
internal expect val ARBS_EXTRAS: Map<String, () -> Arb<String>>
