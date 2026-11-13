package community.flock.wirespec.compiler.core.parse

import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.SpecAnnotation
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ParseHeaderTest {

    private fun parse(vararg files: Pair<String, String>) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(files.map { (name, source) -> ModuleContent(FileUri(name), source) }.toNonEmptyListOrNull()!!)

    private fun single(value: String) = Annotation.Parameter("default", Annotation.Value.Single(value))

    @Test
    fun headerAnnotationsBindToTheSpecAndTheFile() {
        val source =
            // language=ws
            """
            |@spec:OpenAPIServer("https://api.example.com", description: "Production")
            |@file:Owner("customers")
            |@spec:Tag("Customers")
            |
            |@Deprecated
            |type Customer { id: String }
            """.trimMargin()

        val ast = parse("customers.ws" to source).shouldBeRight { it.joinToString { e -> e.message } }

        ast.annotations shouldBe listOf(
            SpecAnnotation(
                FileUri("customers.ws"),
                Annotation("OpenAPIServer", listOf(single("https://api.example.com"), Annotation.Parameter("description", Annotation.Value.Single("Production")))),
            ),
            SpecAnnotation(FileUri("customers.ws"), Annotation("Tag", listOf(single("Customers")))),
        )
        ast.modules.single().annotations shouldBe listOf(Annotation("Owner", listOf(single("customers"))))
        ast.modules.single().statements.single().shouldBeInstanceOf<Type>().annotations shouldBe listOf(Annotation("Deprecated", emptyList()))
    }

    @Test
    fun aFileWithOnlyAHeaderAddsSpecAnnotationsAndNoModule() {
        val api =
            // language=ws
            """
            |@spec:Info(title: "Customer API")
            """.trimMargin()
        val customers =
            // language=ws
            """
            |@spec:Tag("Customers")
            |
            |type Customer { id: String }
            """.trimMargin()

        val ast = parse("api.ws" to api, "customers.ws" to customers).shouldBeRight { it.joinToString { e -> e.message } }

        ast.annotations.map { it.fileUri.value to it.annotation.name } shouldBe listOf("api.ws" to "Info", "customers.ws" to "Tag")
        ast.modules.map { it.fileUri.value } shouldBe listOf("customers.ws")
    }

    @Test
    fun aHeaderMaySpanSeveralLines() {
        val source =
            // language=ws
            """
            |@spec:Server(
            |    "https://api.example.com",
            |    description: "Production"
            |)
            |
            |// The customer
            |type Customer { id: String }
            """.trimMargin()

        parse("customers.ws" to source).shouldBeRight { it.joinToString { e -> e.message } }.annotations shouldHaveSize 1
    }

    @Test
    fun anEmptyLineMustFollowTheHeader() {
        val source =
            // language=ws
            """
            |@spec:Tag("Customers")
            |type Customer { id: String }
            """.trimMargin()

        parse("customers.ws" to source).shouldBeLeft().map { it.message } shouldBe nonEmptyListOf(
            "Leave an empty line between the file header and what follows it",
        )
    }

    @Test
    fun anEmptyLineMustAlsoSeparateTheHeaderFromAComment() {
        val source =
            // language=ws
            """
            |@spec:Tag("Customers")
            |/* The customer */
            |type Customer { id: String }
            """.trimMargin()

        parse("customers.ws" to source).shouldBeLeft().map { it.message } shouldBe nonEmptyListOf(
            "Leave an empty line between the file header and what follows it",
        )
    }

    @Test
    fun aHeaderAnnotationAfterADefinitionIsMisplaced() {
        val source =
            // language=ws
            """
            |type Customer { id: String }
            |
            |@spec:Tag("Customers")
            |
            |type Order { id: String }
            """.trimMargin()

        parse("customers.ws" to source).shouldBeLeft().map { it.message } shouldBe nonEmptyListOf(
            "@spec:Tag belongs in the file header: @spec: and @file: annotations come first in a file, before any definition or comment",
        )
    }

    @Test
    fun aHeaderAnnotationAmongTheAnnotationsOfADefinitionIsMisplaced() {
        val source =
            // language=ws
            """
            |@Deprecated
            |@file:Owner("customers")
            |type Customer { id: String }
            """.trimMargin()

        parse("customers.ws" to source).shouldBeLeft().map { it.message } shouldBe nonEmptyListOf(
            "@file:Owner belongs in the file header: @spec: and @file: annotations come first in a file, before any definition or comment",
        )
    }

    @Test
    fun aCommentCannotComeBeforeTheHeader() {
        val source =
            // language=ws
            """
            |// Licensed under Apache 2.0
            |@spec:Tag("Customers")
            |
            |type Customer { id: String }
            """.trimMargin()

        parse("customers.ws" to source).shouldBeLeft().map { it.message } shouldBe nonEmptyListOf(
            "@spec:Tag belongs in the file header: @spec: and @file: annotations come first in a file, before any definition or comment",
        )
    }

    @Test
    fun aFileWithOnlyAHeaderCannotHoldFileAnnotations() {
        val api =
            // language=ws
            """
            |@spec:Info(title: "Customer API")
            |@file:Owner("platform")
            """.trimMargin()
        val customers =
            // language=ws
            """
            |type Customer { id: String }
            """.trimMargin()

        parse("api.ws" to api, "customers.ws" to customers).shouldBeLeft().map { it.message } shouldBe nonEmptyListOf(
            "A file with only a header can hold @spec: annotations, but no @file: annotations: they describe the definitions in the file",
        )
    }

    @Test
    fun aSpecNeedsADefinitionInSomeFile() {
        val api =
            // language=ws
            """
            |@spec:Info(title: "Customer API")
            """.trimMargin()

        parse("api.ws" to api).shouldBeLeft().map { it.message } shouldBe nonEmptyListOf("AST should not be empty")
    }

    @Test
    fun aSpecAnnotationIsUnique() {
        val api =
            // language=ws
            """
            |@spec:Server("https://api.example.com")
            |@spec:Server("https://api.example.com")
            |@spec:Info(title: "Customer API")
            """.trimMargin()
        val customers =
            // language=ws
            """
            |@spec:Info(title: "Customer API")
            |@spec:Server("https://sandbox.example.com")
            |
            |type Customer { id: String }
            """.trimMargin()

        parse("api.ws" to api, "customers.ws" to customers).shouldBeLeft().map { it.message } shouldBe nonEmptyListOf(
            "@spec:Server(\"https://api.example.com\") is declared 2 times, in api.ws: a spec annotation is unique",
            "@spec:Info is declared 2 times, in api.ws and customers.ws: a spec annotation is unique",
        )
    }
}
