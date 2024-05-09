package community.flock.wirespec.compare

import AddedDefinitionValidated
import AddedFieldTypeValidated
import ChangedFieldTypeValidated
import Compare
import MethodEndpointValidated
import PathEndpointValidated
import RemovedDefinitionValidated
import RemovedFieldTypeValidated
import Validated
import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import kotlin.reflect.KClass
import kotlin.test.Test

class CompareTest {

    @Test
    fun testAddRemoveDefinition() {
        val left = """
            |type Foo {
            |   foo: Number
            |}
            |
            |type Bar {
            |   bar: Number
            |}
        """.trimMargin()

        val right = """
            |type Foo {
            |   foo: Number
            |}
            |
            |type Baz {
            |   baz: Number
            |}
        """.trimMargin()

        val res = Compare.compare(
            left.parse().filterIsInstance<Definition>(),
            right.parse().filterIsInstance<Definition>()
        )

        res.toResult() shouldBeLeft listOf(
            AssertionResult(RemovedDefinitionValidated::class, "Removed definition Bar"),
            AssertionResult(AddedDefinitionValidated::class, "Added definition Baz"),
        )
    }


    @Test
    fun testAddRemoveTypeField() {
        val left = """
            |type Foo {
            |   bar: Number
            |}
        """.trimMargin()

        val right = """
            |type Foo {
            |   baz: Number
            |}
        """.trimMargin()

        val res = Compare.compare(
            left.parse().filterIsInstance<Definition>(),
            right.parse().filterIsInstance<Definition>()
        )

        res.toResult() shouldBeLeft listOf(
            AssertionResult(RemovedFieldTypeValidated::class, "Removed field bar"),
            AssertionResult(AddedFieldTypeValidated::class, "Added field baz"),
        )
    }

    @Test
    fun testChangeReferenceTypeField() {
        val left = """
            |type Foo {
            |   bar: Number
            |}
        """.trimMargin()

        val right = """
            |type Foo {
            |   bar: String[]?
            |}
        """.trimMargin()

        (left to right).compare().toResult() shouldBeLeft listOf(
            AssertionResult(ChangedFieldTypeValidated::class, "Changed field Number to String[]?"),
        )
    }

    @Test
    fun testEndpoint() {
        val left = """
            |endpoint GetTodos GET /todos -> {
            |   200 -> String
            |}
        """.trimMargin()

        val right = """
            |endpoint GetTodos POST /todo -> {
            |   200 -> String
            |}
        """.trimMargin()

        (left to right).compare().toResult() shouldBeLeft listOf(
            AssertionResult(MethodEndpointValidated::class, "Changed method GET to POST"),
            AssertionResult(PathEndpointValidated::class, "Changed path /todos to /todo"),
        )
    }

}


private fun String.parse() = WirespecSpec.parse(this)(noLogger).getOrElse { error("Cannot parse $it") }


fun Pair<String, String>.compare() =
    Compare.compare(
        first.parse().filterIsInstance<Definition>(),
        second.parse().filterIsInstance<Definition>()
    )

fun Either<NonEmptyList<Validated>, *>.toResult() =
    this.mapLeft {
        it.map { AssertionResult(it::class, it.message) }
    }


data class AssertionResult(
    val className: KClass<*>? = null,
    val info: String? = null
)