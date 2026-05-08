package community.flock.wirespec.integration.kotest

import io.kotest.property.Arb
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.uuid
import io.kotest.property.arbs.color
import io.kotest.property.arbs.domain
import io.kotest.property.arbs.firstName
import io.kotest.property.arbs.lastName
import io.kotest.property.arbs.name
import io.kotest.property.arbs.usernames

/**
 * JVM-only default registrations: [Arb.uuid] (which uses `java.util.UUID`) plus
 * the `kotest-property-arbs` extras (which only ship a legacy-JS-compiler
 * artifact). Each Arb ships a wrapper class; we extract the string-shaped
 * property so the registry stays uniformly `Arb<String>`.
 */
internal actual val ARBS_EXTRAS: Map<String, () -> Arb<String>> = mapOf(
    "uuid" to { Arb.uuid().map { it.toString() } },
    "firstName" to { Arb.firstName().map { it.name } },
    "lastName" to { Arb.lastName().map { it.name } },
    "fullName" to { Arb.name().map { "${it.first.name} ${it.last.name}" } },
    "name" to { Arb.name().map { "${it.first.name} ${it.last.name}" } },
    "username" to { Arb.usernames().map { it.value } },
    "domain" to { Arb.domain().map { it.value } },
    "color" to { Arb.color().map { it.value } },
)
