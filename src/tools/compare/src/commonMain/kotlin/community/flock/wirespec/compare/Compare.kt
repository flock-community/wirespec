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
        return list.mapOrAccumulate {
            when (it) {
                is Paired.Left -> RemovedDefinitionValidation(it.left).left().bind()
                is Paired.Right -> AddedDefinitionValidation(it.right).left().bind()
                is Paired.Both -> when {
                    it.left == it.right -> true.right().bind()
                    it.left is Type && it.right is Type -> compareType(it.left, it.right).bindNel()
                    it.left is Endpoint && it.right is Endpoint -> compare(it.left, it.right).bindNel()
                    else -> TODO()
                }
            }
        }
    }

    fun compareType(left: Type, right: Type): Either<NonEmptyList<FieldValidation>, List<Boolean>> {
        val paired = (left.shape.value to right.shape.value).pairBy { it.identifier.value }
        return paired.mapOrAccumulate {
            it.compareField().bindNel()
        }
    }

    private fun Paired<Type.Shape.Field>.compareField(): Either<NonEmptyList<FieldValidation>, Boolean> = when (this) {
        is Paired.Left -> RemovedFieldValidation(left).nel().left()
        is Paired.Right -> AddedFieldValidation(right).nel().left()
        is Paired.Both -> either {
            zipOrAccumulate(
                { ensure(left.isNullable == right.isNullable) { ChangedNullableFieldValidation(left, right) } },
                {
                    ensure(left.reference.isIterable == right.reference.isIterable) {
                        ChangedIterableFieldValidation(
                            left,
                            right
                        )
                    }
                },
                {
                    ensure(left.reference.isMap == right.reference.isMap) {
                        ChangedMapFieldValidation(
                            left,
                            right
                        )
                    }
                },
                {
                    ensure(left.reference.value == right.reference.value) {
                        ChangedReferenceFieldValidation(
                            left,
                            right
                        )
                    }
                }
            ) { _, _, _, _ -> true }
        }
    }


    private fun compare(left: Endpoint, right: Endpoint): Either<NonEmptyList<Validation>, Any> = either {
        zipOrAccumulate(
            { ensure(left.method == right.method) { MethodEndpointValidation(left, right) } },
            { ensure(left.path == right.path) { PathEndpointValidation(left, right) } },
            { (left.query to right.query).pairBy { it.identifier.value }.mapOrAccumulate { it.compareField().bindNel() } },
            { (left.headers to right.headers).pairBy { it.identifier.value }.mapOrAccumulate { it.compareField().bindNel() } },
        )
        { _, _, _, _ -> true }
    }

    sealed class Paired<A> {
        class Left<A>(val key:String, val left: A) : Paired<A>()
        class Right<A>(val key:String, val right: A) : Paired<A>()
        class Both<A>(val key:String, val left: A, val right: A) : Paired<A>()
    }

    inline fun <A> Pair<List<A>, List<A>>.pairBy(f: (a: A) -> String): List<Paired<A>> {
        val leftMap = first.groupBy { f(it) }
        val rightMap = second.groupBy { f(it) }
        val allKeys = leftMap.keys + rightMap.keys
        return allKeys.map {
            when {
                leftMap[it] == null && rightMap[it] != null -> Paired.Right(it, rightMap[it]!!.first())
                leftMap[it] != null && rightMap[it] == null -> Paired.Left(it, leftMap[it]!!.first())
                else -> Paired.Both(it, leftMap[it]!!.first(), rightMap[it]!!.first())
            }
        }.toList()
    }
}

