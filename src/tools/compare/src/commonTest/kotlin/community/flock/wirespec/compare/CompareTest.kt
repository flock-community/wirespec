package community.flock.wirespec.compare

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
            |
            |enum Enum { A, B }
            |
        """.trimMargin()

        val res = Compare.compare(
            left.parse().filterIsInstance<Definition>(),
            right.parse().filterIsInstance<Definition>()
        )

        res.toResult() shouldBeLeft listOf(
            AssertionResult(RemovedDefinitionValidation::class, "Removed definition Bar"),
            AssertionResult(AddedDefinitionValidation::class, "Added definition Baz"),
            AssertionResult(AddedDefinitionValidation::class, "Added definition Enum"),
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
            AssertionResult(RemovedFieldValidation::class, "Removed field bar"),
            AssertionResult(AddedFieldValidation::class, "Added field baz"),
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

        val res = (left to right).compare()

        res.toResult() shouldBeLeft listOf(
            AssertionResult(ChangedNullableFieldValidation::class, "Changed field from nullable false to true"),
            AssertionResult(ChangedIterableFieldValidation::class, "Changed field from iterable false to true"),
            AssertionResult(ChangedReferenceFieldValidation::class, "Changed field Number to String"),
        )
    }

    @Test
    fun testEndpoint() {
        val left = """
            |endpoint GetTodos GET /todos?{done:Boolean}#{x:String} -> {
            |   200 -> String
            |}
        """.trimMargin()

        val right = """
            |endpoint GetTodos POST /todo?{dont:Boolean}#{x:Boolean} -> {
            |   200 -> String
            |}
        """.trimMargin()

        (left to right).compare().toResult() shouldBeLeft listOf(
            AssertionResult(MethodEndpointValidation::class, "Changed method GET to POST"),
            AssertionResult(PathEndpointValidation::class, "Changed path /todos to /todo"),
            AssertionResult(RemovedFieldValidation::class, "Removed field done"),
            AssertionResult(AddedFieldValidation::class, "Added field dont"),
            AssertionResult(ChangedReferenceFieldValidation::class, "Changed field String to Boolean"),
        )
    }

}

private fun String.parse() = WirespecSpec.parse(this)(noLogger).getOrElse { error("Cannot parse $it") }


fun Pair<String, String>.compare() =
    Compare.compare(
        first.parse().filterIsInstance<Definition>(),
        second.parse().filterIsInstance<Definition>()
    )

fun Either<NonEmptyList<Validation>, *>.toResult() =
    this.mapLeft {
        it.map { AssertionResult(it::class, it.message) }
    }


data class AssertionResult(
    val className: KClass<*>? = null,
    val info: String? = null
)