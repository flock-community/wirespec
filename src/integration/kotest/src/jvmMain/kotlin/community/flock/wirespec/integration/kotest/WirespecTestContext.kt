package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec

/**
 * Framework-neutral handle the scenario DSL consumes: a [Wirespec.Transportation]
 * for sending requests and a [Wirespec.Serialization] for typed (de)serialization.
 */
class WirespecTestContext(
    val transportation: Wirespec.Transportation,
    val serialization: Wirespec.Serialization,
)
