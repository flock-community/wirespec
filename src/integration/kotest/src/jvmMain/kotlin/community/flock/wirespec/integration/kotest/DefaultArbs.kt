package community.flock.wirespec.integration.kotest

import io.kotest.property.Arb
import io.kotest.property.arbitrary.email
import io.kotest.property.arbitrary.ipAddressV4
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.uuid
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
 * Source libraries:
 * - **kotest-property** (core): `email`, `uuid`, `ipAddress`.
 * - **kotest-property-arbs** (extras): `firstName`, `lastName`, `fullName`
 *   (= `name`), `username`, `domain`, `color`.
 *
 * Override an entry with `register("name") { … }` in the builder block.
 */
internal val DEFAULT_ARBS: Map<String, () -> Arb<String>> = mapOf(
    "email" to { Arb.email() },
    "uuid" to { Arb.uuid().map { it.toString() } },
    "ipAddress" to { Arb.ipAddressV4() },
    "firstName" to { Arb.firstName().map { it.name } },
    "lastName" to { Arb.lastName().map { it.name } },
    "fullName" to { Arb.name().map { "${it.first.name} ${it.last.name}" } },
    "username" to { Arb.usernames().map { it.value } },
    "domain" to { Arb.domain().map { it.value } },
    "color" to { Arb.color().map { it.value } },
)
