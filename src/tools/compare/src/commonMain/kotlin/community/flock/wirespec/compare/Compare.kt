import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.mapOrAccumulate
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union


sealed interface Validated {
    val braking: Boolean
    val message: String
}

sealed interface DefinitionValidated : Validated {
    val definition: Definition
}

data class RemovedDefinitionValidated(
    override val definition: Definition
) : DefinitionValidated {
    override val braking: Boolean = true
    override val message: String = "Removed definition ${definition.name}"
}

data class AddedDefinitionValidated(
    override val definition: Definition
) : DefinitionValidated {
    override val braking: Boolean = false
    override val message: String = "Added definition ${definition.name}"
}

sealed interface TypeValidated : Validated {
    val field: Paired<Type.Shape.Field>
}

data class RemovedFieldTypeValidated(
    override val field: Paired<Type.Shape.Field>,
) : TypeValidated {
    override val braking = true
    override val message = "Removed field ${field.left?.identifier?.value}"
}

data class AddedFieldTypeValidated(
    override val field: Paired<Type.Shape.Field>,
) : TypeValidated {
    override val braking = true
    override val message = "Added field ${field.right?.identifier?.value}"
}

data class ChangedFieldTypeValidated(
    override val field: Paired<Type.Shape.Field>
) : TypeValidated {
    override val braking = false
    override val message = "Changed field ${field.left?.printReference()} to ${field.right?.printReference()}"
}

sealed interface EndpointValidated : Validated {
    val endpoint: Paired<Endpoint>
}

data class PathEndpointValidated(
    override val endpoint: Paired<Endpoint>
) : EndpointValidated {
    override val braking = true
    override val message = "Changed path ${endpoint.left?.printPath()} to ${endpoint.right?.printPath()}"
}

data class MethodEndpointValidated(
    override val endpoint: Paired<Endpoint>
) : EndpointValidated {
    override val braking = true
    override val message = "Changed method ${endpoint.left?.method} to ${endpoint.right?.method}"
}


data object Unknown : Validated {
    override val braking: Boolean
        get() = TODO("Not yet implemented")
    override val message: String
        get() = TODO("Not yet implemented")
}

object Compare {

    fun compare(left: List<Definition>, right: List<Definition>): Either<NonEmptyList<Validated>, List<Any>> {
        val list = (left to right).pairBy { it.name }
        return list.mapOrAccumulate {
            when {
                it.left == it.right -> true.right().bind()
                it.right == null -> RemovedDefinitionValidated(it.left!!).left().bind()
                it.left == null -> AddedDefinitionValidated(it.right!!).left().bind()
                it.left != it.right -> compareDefinition(it).bindNel()
                else -> Unknown.left().bind()
            }
        }
    }

    private fun compareDefinition(paired: Paired<Definition>): Either<NonEmptyList<Validated>, Any> {
        return when (paired.left) {
            is Type -> compareType(paired as Paired<Type>)
            is Endpoint -> compareEndpoint(paired as Paired<Endpoint>)
            is Enum -> TODO()
            is Refined -> TODO()
            is Union -> TODO()
            null -> TODO()
        }
    }

    fun compareType(paired: Paired<Type>): Either<NonEmptyList<TypeValidated>, Any> {
        val list =
            (paired.left?.shape?.value.orEmpty() to paired.right?.shape?.value.orEmpty()).pairBy { it.identifier.value }
        return list.mapOrAccumulate {
            when {
                it.left == it.right -> true.right().bind()
                it.right == null -> RemovedFieldTypeValidated(it).left().bind()
                it.left == null -> AddedFieldTypeValidated(it).left().bind()
                it.left.isNullable != it.right.isNullable -> ChangedFieldTypeValidated(it).left().bind()
                it.left.reference != it.right.reference -> ChangedFieldTypeValidated(it).left().bind()
                else -> error("")
            }
        }
    }

    fun compareEndpoint(paired: Paired<Endpoint>): Either<NonEmptyList<EndpointValidated>, Any> = either {
        val list = buildList {
            if (paired.left?.method != paired.right?.method) add(MethodEndpointValidated(paired))
            if (paired.left?.path != paired.right?.path) add(PathEndpointValidated(paired))
        }
        if(list.isNotEmpty())
            raise(list.toNonEmptyListOrNull() ?: error(""))
        else
            Either.Right(true).bind()
    }
}

data class Paired<A>(val left: A?, val right: A?)

private fun <A> Pair<List<A>, List<A>>.pairBy(f: (a: A) -> Any): List<Paired<A>> {
    val leftMap = first.groupBy { f(it) }
    val rightMap = second.groupBy { f(it) }
    val allKeys = leftMap.keys + rightMap.keys
    return allKeys.map {
        when {
            leftMap.containsKey(it) && rightMap.containsKey(it) -> Paired(leftMap[it]!!.first(), rightMap[it]!!.first())
            leftMap.containsKey(it) -> Paired(leftMap[it]!!.first(), null)
            rightMap.containsKey(it) -> Paired(null, rightMap[it]!!.first())
            else -> error("")
        }
    }
}

fun Type.Shape.Field.printReference() = buildString {
    append(reference.value)
    if (reference.isIterable) append("[]")
    if (isNullable) append("?")
}

fun Endpoint.printPath() = "/" + path.map {
    when (it) {
        is Endpoint.Segment.Literal -> it.value
        is Endpoint.Segment.Param -> it.identifier.value
    }
}.joinToString("/")