package community.flock.wirespec.compare

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.mapOrAccumulate
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import arrow.core.right
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Type


object Compare {

    fun compare(left: List<Definition>, right: List<Definition>): Either<NonEmptyList<Validation>, List<Any>> {
        val list = (left to right).pairBy { it.name }
        return list.mapOrAccumulate { it.compareDefinition().bindNel() }
    }

    private fun Paired<Definition>.compareDefinition(): Either<NonEmptyList<Validation>, Any> =
        when (this) {
            is Paired.Left -> RemovedDefinitionValidation(key, left).nel().left()
            is Paired.Right -> AddedDefinitionValidation(key, right).nel().left()
            is Paired.Both -> when {
                left == right -> true.right()
                left is Type && right is Type -> (this as Paired.Both<Type>).compareType()
                left is Endpoint && right is Endpoint -> (this as Paired.Both<Endpoint>).compareEndpoint()
                else -> TODO()
            }
        }

    private fun Paired.Both<Type>.compareType(): Either<NonEmptyList<FieldValidation>, List<Boolean>> {
        val paired = (left.shape.value to right.shape.value).pairBy { it.identifier.value }
        return paired.mapOrAccumulate { it.compareField(key).bindNel() }
    }

    private fun Paired<Type.Shape.Field>.compareField(definitionKey: String): Either<NonEmptyList<FieldValidation>, Boolean> =
        when (this) {
            is Paired.Left -> RemovedFieldValidation("${definitionKey}.${key}", left).nel().left()
            is Paired.Right -> AddedFieldValidation("${definitionKey}.${key}", right).nel().left()
            is Paired.Both -> either {
                zipOrAccumulate(
                    {
                        ensure(left.isNullable == right.isNullable) {
                            ChangedNullableFieldValidation("${definitionKey}.${key}", left, right)
                        }
                    },
                    {
                        into { it.reference }.compareReference("${definitionKey}.${key}")
                    },
                ) { _, _ -> true }
            }
        }

    private fun Paired<Type.Shape.Field.Reference>.compareReference(definitionKey: String): Either<NonEmptyList<ReferenceValidation>, Boolean> =
        when (this) {
            is Paired.Left -> RemovedReferenceValidation("${definitionKey}.${key}", left).nel().left()
            is Paired.Right -> AddedReferenceValidation("${definitionKey}.${key}", right).nel().left()
            is Paired.Both -> either {
                zipOrAccumulate(
                    {
                        ensure(left.isIterable == right.isIterable) {
                            ChangedIterableReferenceValidation("${definitionKey}.${key}", left, right)
                        }
                    },
                    {
                        ensure(left.isMap == right.isMap) {
                            ChangedMapReferenceValidation("${definitionKey}.${key}", left, right)
                        }
                    },
                    {
                        ensure(left.value == right.value) {
                            ChangedValueReferenceValidation("${definitionKey}.${key}", left, right)
                        }
                    }
                ) { _, _, _ -> true }
            }
        }

    private fun Paired<Endpoint.Request>.compareRequest(definitionKey: String): Either<NonEmptyList<Validation>, Boolean> =
        when (this) {
            is Paired.Left -> RemovedRequestValidation("${definitionKey}.${key}", left).nel().left()
            is Paired.Right -> AddedRequestValidation("${definitionKey}.${key}", right).nel().left()
            is Paired.Both -> when {
                left.content ==  right.content -> true.right()
                else -> into { it.content ?: error("") }.compareContent("${definitionKey}.${key}")
            }
        }

    private fun Paired<Endpoint.Content>.compareContent(definitionKey: String): Either<NonEmptyList<ContentValidation>, Boolean> =
        when (this) {
            is Paired.Left -> RemovedContentValidation("${definitionKey}.${key}", left).nel().left()
            is Paired.Right -> AddedContentValidation("${definitionKey}.${key}", right).nel().left()
            is Paired.Both -> either {
                zipOrAccumulate(
                    {
                        ensure(left.type == right.type) {
                            ChangedTypeContentValidation(
                                "${definitionKey}.${key}",
                                left,
                                right
                            )
                        }
                    },
                    {
                        into { it.reference }.compareReference("${definitionKey}.${key}")
                    },
                    { _, _ -> true }
                )
            }
        }

    private fun Paired.Both<Endpoint>.compareEndpoint(): Either<NonEmptyList<Validation>, Any> = either {
        zipOrAccumulate(
            { ensure(left.method == right.method) { MethodEndpointValidation(key, left, right) } },
            { ensure(left.path == right.path) { PathEndpointValidation(key, left, right) } },
            {
                (left.query to right.query).pairBy { it.identifier.value }
                    .mapOrAccumulate { it.compareField(key).bindNel() }
            },
            {
                (left.headers to right.headers).pairBy { it.identifier.value }
                    .mapOrAccumulate { it.compareField(key).bindNel() }
            },
            {
                (left.cookies to right.cookies).pairBy { it.identifier.value }
                    .mapOrAccumulate { it.compareField(key).bindNel() }
            },
            {
                (left.requests to right.requests).pairBy { it.content?.type.orEmpty() }
                    .mapOrAccumulate { it.compareRequest(key).bindNel() }
            },
        )
        { _, _, _, _, _, _ -> true }
    }

}

