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
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Type


object Compare {

    fun compare(left: List<Definition>, right: List<Definition>): Either<NonEmptyList<Difference>, List<Any>> {
        val list = (left to right).pairBy { it.identifier.value }
        return list.mapOrAccumulate { it.compareDefinition().bindNel() }
    }

    private fun Paired<Definition>.compareDefinition(): Either<NonEmptyList<Difference>, Any> =
        when (this) {
            is Paired.Old -> RemovedDefinitionDifference(key, old).nel().left()
            is Paired.New -> AddedDefinitionDifference(key, new).nel().left()
            is Paired.Both -> when {
                old == new -> true.right()
                old is Type && new is Type -> (this as Paired.Both<Type>).compareType()
                old is Endpoint && new is Endpoint -> (this as Paired.Both<Endpoint>).compareEndpoint()
                else -> TODO()
            }
        }

    private fun Paired.Both<Type>.compareType(): Either<NonEmptyList<Difference>, List<Boolean>> {
        val paired = (old.shape.value to new.shape.value).pairBy { it.identifier.value }
        return paired.mapOrAccumulate { it.compareField(key).bindNel() }
    }

    private fun Paired<Field>.compareField(definitionKey: String): Either<NonEmptyList<Difference>, Boolean> =
        when (this) {
            is Paired.Old -> RemovedFieldDifference("${definitionKey}.${key}", old).nel().left()
            is Paired.New -> AddedFieldDifference("${definitionKey}.${key}", new).nel().left()
            is Paired.Both ->
                either {
                    zipOrAccumulate(
                        {
                            ensure(old.isNullable == new.isNullable) {
                                ChangedNullableFieldDifference("${definitionKey}.${key}", old, new)
                            }
                        },
                        {
                            into { it.reference }.compareReference("${definitionKey}.${key}").bindNel()
                        },
                    ) { _, _ -> true }
                }
        }

    private fun Paired<Field.Reference>.compareReference(definitionKey: String): Either<NonEmptyList<ReferenceDifference>, Boolean> =
        when (this) {
            is Paired.Old -> RemovedReferenceDifference("${definitionKey}.${key}", old).nel().left()
            is Paired.New -> AddedReferenceDifference("${definitionKey}.${key}", new).nel().left()
            is Paired.Both -> either {
                zipOrAccumulate(
                    {
                        ensure(old.isIterable == new.isIterable) {
                            ChangedIterableReferenceDifference(definitionKey, old, new)
                        }
                    },
                    {
                        ensure(old.isDictionary == new.isDictionary) {
                            ChangedMapReferenceDifference(definitionKey, old, new)
                        }
                    },
                    {
                        ensure(old.value == new.value) {
                            ChangedValueReferenceDifference(definitionKey, old, new)
                        }
                    }
                ) { _, _, _ -> true }
            }
        }

    private fun Paired<Endpoint.Request>.compareRequest(definitionKey: String): Either<NonEmptyList<Difference>, Boolean> =
        when (this) {
            is Paired.Old -> RemovedRequestDifference("${definitionKey}.${key}", old).nel().left()
            is Paired.New -> AddedRequestDifference("${definitionKey}.${key}", new).nel().left()
            is Paired.Both -> when {
                old.content == new.content -> true.right()
                else -> into { it.content ?: error("") }.compareContent("${definitionKey}.${key}")
            }
        }

    private fun Paired<Endpoint.Content>.compareContent(definitionKey: String): Either<NonEmptyList<Difference>, Boolean> =
        when (this) {
            is Paired.Old -> RemovedContentDifference("${definitionKey}.${key}", old).nel().left()
            is Paired.New -> AddedContentDifference("${definitionKey}.${key}", new).nel().left()
            is Paired.Both -> either {
                zipOrAccumulate(
                    {
                        ensure(old.type == new.type) {
                            ChangedTypeContentDifference(
                                "${definitionKey}.${key}",
                                old,
                                new
                            )
                        }
                    },
                    {
                        into { it.reference }.compareReference("${definitionKey}.${key}").bindNel()
                    },
                    { _, _ -> true }
                )
            }
        }

    private fun Paired.Both<Endpoint>.compareEndpoint(): Either<NonEmptyList<Difference>, Any> = either {
        zipOrAccumulate(
            { ensure(old.method == new.method) { MethodEndpointDifference(key, old, new) } },
            { ensure(old.path == new.path) { PathEndpointDifference(key, old, new) } },
            {
                (old.query to new.query).pairBy { it.identifier.value }
                    .mapOrAccumulate { it.compareField(key).bindNel() }
            },
            {
                (old.headers to new.headers).pairBy { it.identifier.value }
                    .mapOrAccumulate { it.compareField(key).bindNel() }
            },
            {
                (old.cookies to new.cookies).pairBy { it.identifier.value }
                    .mapOrAccumulate { it.compareField(key).bindNel() }
            },
            {
                (old.requests to new.requests).pairBy { it.content?.type.orEmpty() }
                    .mapOrAccumulate { it.compareRequest(key).bindNel() }
            },
        )
        { _, _, _, _, _, _ -> true }
    }

}

