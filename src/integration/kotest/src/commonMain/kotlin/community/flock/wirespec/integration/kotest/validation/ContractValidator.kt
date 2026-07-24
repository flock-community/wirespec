package community.flock.wirespec.integration.kotest.validation

import community.flock.wirespec.kotlin.Wirespec

/** Kind of contract violation surfaced to test failure messages. */
enum class ContractViolationKind { UndeclaredStatus, BodyMismatch }

/** Validates a [Wirespec.RawResponse] against an endpoint's contract (status code, then body schema). */
internal class ContractValidator(
    private val endpoint: EndpointReflection,
    private val serialization: Wirespec.Serialization,
) {

    fun validate(raw: Wirespec.RawResponse): Any {
        val variant = endpoint.responseClassForStatus(raw.statusCode)
            ?: throw ContractViolation(
                endpoint = endpoint.endpointName,
                kind = ContractViolationKind.UndeclaredStatus,
                message = "status ${raw.statusCode} is not declared by the contract. " +
                    "Declared: ${endpoint.responseVariantsByStatus.keys.sorted()}",
                rawResponse = raw,
            )

        return try {
            endpoint.fromRawResponse(serialization, raw)
        } catch (t: Throwable) {
            throw ContractViolation(
                endpoint = endpoint.endpointName,
                kind = ContractViolationKind.BodyMismatch,
                message = "body did not match ${variant.simpleName}: ${t.message ?: t::class.simpleName}",
                rawResponse = raw,
                cause = t,
            )
        }
    }
}

class ContractViolation internal constructor(
    val endpoint: String,
    val kind: ContractViolationKind,
    message: String,
    val rawResponse: Wirespec.RawResponse,
    cause: Throwable? = null,
) : AssertionError(
    buildString {
        append("[$endpoint] ").append(kind).append(": ").append(message)
        rawResponse.body?.let { body -> append("\n  body=").append(String(body).take(2048)) }
    },
    cause,
)
