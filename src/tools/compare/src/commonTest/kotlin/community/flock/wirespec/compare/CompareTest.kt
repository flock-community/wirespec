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
            AssertionResult(RemovedDefinitionDifference::class, "Bar", "Removed definition Bar"),
            AssertionResult(AddedDefinitionDifference::class, "Baz", "Added definition Baz"),
            AssertionResult(AddedDefinitionDifference::class, "Enum", "Added definition Enum"),
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
            AssertionResult(RemovedFieldDifference::class, "Foo.bar", "Removed field bar"),
            AssertionResult(AddedFieldDifference::class, "Foo.baz", "Added field baz"),
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
            AssertionResult(
                ChangedNullableFieldDifference::class,
                "Foo.bar",
                "Changed field from nullable false to true"
            ),
            AssertionResult(
                ChangedIterableReferenceDifference::class,
                "Foo.bar",
                "Changed field from iterable false to true"
            ),
            AssertionResult(
                ChangedValueReferenceDifference::class,
                "Foo.bar",
                "Changed field Number to String"
            ),
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
            AssertionResult(MethodEndpointDifference::class, "GetTodos", "Changed method GET to POST"),
            AssertionResult(PathEndpointDifference::class, "GetTodos", "Changed path /todos to /todo"),
            AssertionResult(RemovedFieldDifference::class, "GetTodos.done", "Removed field done"),
            AssertionResult(AddedFieldDifference::class, "GetTodos.dont", "Added field dont"),
            AssertionResult(ChangedValueReferenceDifference::class, "GetTodos.x", "Changed field String to Boolean"),
        )
    }
}

private fun String.parse() = WirespecSpec.parse(this)(noLogger).getOrElse { error("Cannot parse $it") }


fun Pair<String, String>.compare() =
    Compare.compare(
        first.parse().filterIsInstance<Definition>(),
        second.parse().filterIsInstance<Definition>()
    )

fun Either<NonEmptyList<Difference>, *>.toResult() =
    this.mapLeft {
        it.map { AssertionResult(it::class, it.key, it.message) }
    }


data class AssertionResult(
    val className: KClass<*>?,
    val key: String,
    val info: String?
)
