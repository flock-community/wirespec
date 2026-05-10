package community.flock.wirespec.integration.kotest

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.email
import io.kotest.property.arbitrary.ipAddressV4
import io.kotest.property.arbitrary.map
import io.kotest.property.arbs.color
import io.kotest.property.arbs.domain
import io.kotest.property.arbs.firstName
import io.kotest.property.arbs.lastName
import io.kotest.property.arbs.name
import io.kotest.property.arbs.usernames

/**
 * Default catalog of `@Generator("name")` registrations preinstalled on every
 * `kotestWirespecGenerator` instance. Keys mirror the source `Arb.xxx()` names
 * for readability; lookup is case-insensitive (the registry lowercases keys on
 * `register`), so `@Generator("firstName")`, `@Generator("firstname")`, and
 * `@Generator("FIRSTNAME")` all resolve to the same entry.
 *
 * Source libraries (all multiplatform):
 * - **kotest-property** core: `email`, `ipAddress`.
 * - **kotest-property-arbs** (3.x): `firstName`, `lastName`, `fullName` (= `name`),
 *   `username`, `domain`, `color`.
 * - In-house portable RFC 4122 v4 generator: `uuid`. (`Arb.uuid()` from
 *   kotest-property core is JVM-only because it wraps `java.util.UUID`.)
 *
 * Override an entry with `register("name") { … }` in the builder block.
 */
internal val DEFAULT_ARBS: Map<String, () -> Arb<String>> = mapOf(
    "email" to { Arb.email() },
    "ipAddress" to { Arb.ipAddressV4() },
    "uuid" to { uuidArb() },
    "firstName" to { Arb.firstName().map { it.name } },
    "lastName" to { Arb.lastName().map { it.name } },
    "fullName" to { Arb.name().map { "${it.first.name} ${it.last.name}" } },
    "name" to { Arb.name().map { "${it.first.name} ${it.last.name}" } },
    "username" to { Arb.usernames().map { it.value } },
    "domain" to { Arb.domain().map { it.value } },
    "color" to { Arb.color().map { it.value } },
)

/**
 * Portable RFC 4122 v4 UUID arb that draws every nibble from the supplied
 * `RandomSource`, so the same `kotestWirespecGenerator(seed)` produces the
 * same UUIDs across JVM and JS. Format: `xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx`
 * with `y` ∈ `{8,9,a,b}` (variant 1).
 */
private fun uuidArb(): Arb<String> = arbitrary { rs ->
    val r = rs.random
    fun hex(n: Int): String = buildString(n) {
        repeat(n) {
            val v = r.nextInt(16)
            append(if (v < 10) '0' + v else 'a' + (v - 10))
        }
    }
    val timeLow = hex(8)
    val timeMid = hex(4)
    val timeHi = "4" + hex(3)
    val variant = "89ab"[r.nextInt(4)]
    val clockSeq = variant + hex(3)
    val node = hex(12)
    "$timeLow-$timeMid-$timeHi-$clockSeq-$node"
}
