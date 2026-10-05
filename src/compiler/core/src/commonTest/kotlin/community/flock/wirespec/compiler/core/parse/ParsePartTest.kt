package community.flock.wirespec.compiler.core.parse

import arrow.core.EitherNel
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.exceptions.AnnotatedSpreadException
import community.flock.wirespec.compiler.core.exceptions.CyclicPartError
import community.flock.wirespec.compiler.core.exceptions.DefinitionNotExistsException
import community.flock.wirespec.compiler.core.exceptions.DuplicateFieldError
import community.flock.wirespec.compiler.core.exceptions.DuplicatePartError
import community.flock.wirespec.compiler.core.exceptions.PartAsReferenceError
import community.flock.wirespec.compiler.core.exceptions.SpreadNonPartError
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Spread
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ParsePartTest {

    private fun parse(vararg sources: String): EitherNel<WirespecException, AST> = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(
        sources
            .mapIndexed { index, source -> ModuleContent(FileUri("test$index.ws"), source) }
            .toNonEmptyListOrNull()!!,
    )

    private fun AST.definitions(): List<Definition> = modules.flatMap(Module::statements)

    private inline fun <reified T : Definition> AST.single(name: String): T = definitions()
        .filterIsInstance<T>()
        .single { it.identifier.value == name }

    private fun List<Field>.names() = map { it.identifier.value }

    @Test
    fun spreadIsTheSameAsWritingTheFieldsOut() {
        val spread = parse(
            // language=ws
            """
            |part Audit { createdAt: String, updatedAt: String? }
            |type User { ...Audit, name: String }
            """.trimMargin(),
        ).shouldBeRight()

        val handWritten = parse(
            // language=ws
            """
            |type User { createdAt: String, updatedAt: String?, name: String }
            """.trimMargin(),
        ).shouldBeRight()

        spread.single<Type>("User").shape.value shouldBe handWritten.single<Type>("User").shape.value
    }

    @Test
    fun spreadKeepsItsPositionInTheFieldOrder() {
        parse(
            // language=ws
            """
            |part Audit { createdAt: String, updatedAt: String }
            |type User { id: String, ...Audit, name: String }
            """.trimMargin(),
        )
            .shouldBeRight()
            .single<Type>("User").shape
            .run {
                value.names() shouldContainExactly listOf("id", "createdAt", "updatedAt", "name")
                entries.map { (it as? Spread)?.identifier?.value ?: (it as Field).identifier.value } shouldContainExactly listOf("id", "Audit", "name")
            }
    }

    @Test
    fun partsCanSpreadParts() {
        parse(
            // language=ws
            """
            |part Id { id: String }
            |part Audit { ...Id, createdAt: String }
            |type User { ...Audit, name: String }
            """.trimMargin(),
        )
            .shouldBeRight()
            .run {
                single<Type>("User").shape.value.names() shouldContainExactly listOf("id", "createdAt", "name")
                parts.single { it.identifier.value == "Audit" }.shape.value.names() shouldContainExactly listOf("id", "createdAt")
            }
    }

    @Test
    fun spreadCopiesFieldAnnotations() {
        parse(
            // language=ws
            """
            |part Legacy { @Deprecated code: String }
            |type User { ...Legacy }
            """.trimMargin(),
        )
            .shouldBeRight()
            .single<Type>("User").shape.value
            .single().annotations shouldBe listOf(Annotation("Deprecated", emptyList()))
    }

    @Test
    fun spreadIntoEndpointQueryAndHeaders() {
        parse(
            // language=ws
            """
            |part Paging { page: Integer, size: Integer }
            |part Tracing { `X-Trace-Id`: String }
            |endpoint ListUsers GET /users ?{ ...Paging, active: Boolean } #{ ...Tracing } -> {
            |    200 -> String #{ ...Tracing, total: Integer }
            |}
            """.trimMargin(),
        )
            .shouldBeRight()
            .single<Endpoint>("ListUsers")
            .run {
                queries.names() shouldContainExactly listOf("page", "size", "active")
                headers.names() shouldContainExactly listOf("X-Trace-Id")
                responses.single().headers.names() shouldContainExactly listOf("X-Trace-Id", "total")
                queryEntries.first() shouldBe Spread(DefinitionIdentifier("Paging"))
            }
    }

    @Test
    fun spreadIntoRpc() {
        parse(
            // language=ws
            """
            |part Id { id: String }
            |rpc FindUser { ...Id } -> String
            """.trimMargin(),
        )
            .shouldBeRight()
            .single<Rpc>("FindUser").shape.value.names() shouldContainExactly listOf("id")
    }

    @Test
    fun emptyPartAndSpreadOnlyType() {
        parse(
            // language=ws
            """
            |part Empty {}
            |part Id { id: String }
            |type User { ...Empty, ...Id }
            """.trimMargin(),
        )
            .shouldBeRight()
            .single<Type>("User").shape.value.names() shouldContainExactly listOf("id")
    }

    @Test
    fun partsResolveAcrossFilesAndAFileMayHoldOnlyParts() {
        parse(
            // language=ws
            """
            |part Audit { createdAt: String }
            """.trimMargin(),
            // language=ws
            """
            |type User { ...Audit, name: String }
            """.trimMargin(),
        )
            .shouldBeRight()
            .run {
                modules shouldHaveSize 1
                parts.map { it.identifier.value } shouldContainExactly listOf("Audit")
                single<Type>("User").shape.value.names() shouldContainExactly listOf("createdAt", "name")
            }
    }

    @Test
    fun onlyPartsIsAnEmptyAst() {
        parse("part Audit { createdAt: String }").shouldBeLeft()
    }

    @Test
    fun spreadMustReferToSomethingDefined() {
        parse("type User { ...Audit }")
            .shouldBeLeft()
            .single().shouldBeInstanceOf<DefinitionNotExistsException>()
    }

    @Test
    fun onlyPartsCanBeSpread() {
        parse(
            // language=ws
            """
            |type Base { id: String }
            |type User { ...Base }
            """.trimMargin(),
        )
            .shouldBeLeft()
            .single().shouldBeInstanceOf<SpreadNonPartError>()
            .message shouldBe "Cannot spread 'Base' into type User: only parts can be spread"
    }

    @Test
    fun spreadCannotBeAnnotated() {
        parse(
            // language=ws
            """
            |part Audit { createdAt: String }
            |type User { @Deprecated ...Audit }
            """.trimMargin(),
        )
            .shouldBeLeft()
            .single().shouldBeInstanceOf<AnnotatedSpreadException>()
    }

    @Test
    fun spreadFieldCannotCollideWithOwnField() {
        parse(
            // language=ws
            """
            |part Audit { id: String }
            |type User { ...Audit, id: Integer }
            """.trimMargin(),
        )
            .shouldBeLeft()
            .single().shouldBeInstanceOf<DuplicateFieldError>()
            .message shouldBe "Field 'id' is defined more than once in type User"
    }

    @Test
    fun diamondIsACollision() {
        parse(
            // language=ws
            """
            |part Id { id: String }
            |part Audit { ...Id, createdAt: String }
            |part Owned { ...Id, owner: String }
            |type Doc { ...Audit, ...Owned }
            """.trimMargin(),
        )
            .shouldBeLeft()
            .single().shouldBeInstanceOf<DuplicateFieldError>()
            .message shouldBe "Field 'id' is defined more than once in type Doc"
    }

    @Test
    fun collisionInsideAPartIsReportedOnceOnThePart() {
        parse(
            // language=ws
            """
            |part Id { id: String }
            |part Twice { ...Id, id: String }
            |type Doc { ...Twice }
            """.trimMargin(),
        )
            .shouldBeLeft()
            .single().message shouldBe "Field 'id' is defined more than once in part Twice"
    }

    @Test
    fun headerCollisionsAreCaseSensitive() {
        parse(
            // language=ws
            """
            |part Tracing { `X-Trace`: String }
            |endpoint Ping GET /ping #{ ...Tracing, `x-trace`: String } -> {
            |    200 -> Unit
            |}
            """.trimMargin(),
        ).shouldBeRight()
    }

    @Test
    fun partsCannotBeCyclic() {
        parse(
            // language=ws
            """
            |part A { ...B }
            |part B { ...A }
            |part C { ...C }
            |type User { ...A }
            """.trimMargin(),
        )
            .shouldBeLeft()
            .single().shouldBeInstanceOf<CyclicPartError>()
            .message shouldBe "Parts cannot spread themselves, directly or indirectly: A, B, C"
    }

    @Test
    fun partCannotBeUsedAsAType() {
        parse(
            // language=ws
            """
            |part Audit { createdAt: String }
            |type User { audit: Audit[] }
            |type Admin = User | Audit
            |endpoint Create POST Audit /audits -> {
            |    200 -> Audit
            |}
            """.trimMargin(),
        )
            .shouldBeLeft()
            .single().shouldBeInstanceOf<PartAsReferenceError>()
            .message shouldBe "Part 'Audit' cannot be used as a type; spread it into a shape with ...Audit"
    }

    @Test
    fun partSharesTheTypeNamespace() {
        parse(
            // language=ws
            """
            |part User { id: String }
            |part Id { id: String }
            |part Id { id: String }
            |type User { name: String }
            """.trimMargin(),
        )
            .shouldBeLeft()
            .map { it.shouldBeInstanceOf<DuplicatePartError>().message } shouldContainExactly listOf(
            "Part 'User' is already defined",
            "Part 'Id' is already defined",
        )
    }

    @Test
    fun partIsAKeyword() {
        parse("type Shipment { part: String }").shouldBeLeft()
        parse("type Shipment { `part`: String, partner: String }").shouldBeRight()
    }

    @Test
    fun warnsAboutUnusedParts() {
        val warnings = mutableListOf<String>()
        object : ParseContext {
            override val spec = WirespecSpec
            override val logger = object : Logger(logLevel = null) {
                override fun warn(string: String) {
                    warnings.add(string)
                }
            }
        }.parse(
            nonEmptyListOf(
                ModuleContent(
                    FileUri("test.ws"),
                    // language=ws
                    """
                    |part Used { a: String }
                    |part Unused { b: String }
                    |type User { ...Used }
                    """.trimMargin(),
                ),
            ),
        ).shouldBeRight()

        warnings shouldContainExactly listOf("Part 'Unused' is never spread")
    }

    @Test
    fun noPartsLeavesTheAstUntouched() {
        parse("type User { name: String }")
            .shouldBeRight()
            .run {
                parts.shouldBeEmpty()
                single<Type>("User").shape shouldBe Type.Shape(single<Type>("User").shape.value)
            }
    }
}
