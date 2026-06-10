package community.flock.wirespec.ir.converter

import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.generator.Generator
import community.flock.wirespec.ir.generator.JavaGenerator
import community.flock.wirespec.ir.generator.KotlinGenerator
import community.flock.wirespec.ir.generator.PythonGenerator
import community.flock.wirespec.ir.generator.RustGenerator
import community.flock.wirespec.ir.generator.ScalaGenerator
import community.flock.wirespec.ir.generator.TypeScriptGenerator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeWirespec

class UnderscoreTypeNameGeneratorTest {

    // Wirespec type identifiers may contain underscores (e.g. the synthetic
    // `ProposalNow_embedded` from an OpenAPI HAL `_embedded` field). Emitters
    // render definitions via `pascalCase()`, which drops the separator, so
    // every reference must agree or the generated code names something that
    // was never declared.
    private val generators: List<Pair<String, Generator>> = listOf(
        "Kotlin" to KotlinGenerator,
        "Java" to JavaGenerator,
        "Scala" to ScalaGenerator,
        "Rust" to RustGenerator,
        "Python" to PythonGenerator,
        "TypeScript" to TypeScriptGenerator,
    )

    private fun typeDef(name: String, shape: List<Field>) = TypeWirespec(
        comment = null,
        annotations = emptyList(),
        identifier = DefinitionIdentifier(name),
        shape = TypeWirespec.Shape(shape),
        extends = emptyList(),
    )

    @Test
    fun underscoreBearingReferencesArePascalCasedInEveryGenerator() {
        val parent = typeDef(
            "ProposalNow",
            listOf(
                Field(
                    emptyList(),
                    FieldIdentifier("_embedded"),
                    Reference.Custom("ProposalNow_embedded", true),
                ),
            ),
        )
        val nested = typeDef(
            "ProposalNow_embedded",
            listOf(
                Field(
                    emptyList(),
                    FieldIdentifier("name"),
                    Reference.Primitive(Reference.Primitive.Type.String(null), false),
                ),
            ),
        )

        for ((lang, generator) in generators) {
            val parentCode = generator.generate(parent.convertToGenerator() as Element)
            val nestedCode = generator.generate(nested.convertToGenerator() as Element)

            assertTrue(
                parentCode.contains("ProposalNowEmbedded"),
                "$lang: expected pascal-cased reference, got:\n$parentCode",
            )
            assertFalse(
                parentCode.contains("ProposalNow_embedded"),
                "$lang: underscore leaked into a reference:\n$parentCode",
            )
            assertFalse(
                nestedCode.contains("ProposalNow_embedded"),
                "$lang: underscore leaked into the nested generator:\n$nestedCode",
            )
        }
    }

    @Test
    fun underscoreFieldNameItselfIsPreserved() {
        // The JSON field name `_embedded` keeps its leading underscore —
        // only the *type* name is normalised.
        val parent = typeDef(
            "ProposalNow",
            listOf(
                Field(
                    emptyList(),
                    FieldIdentifier("_embedded"),
                    Reference.Custom("ProposalNow_embedded", true),
                ),
            ),
        )
        val kotlin = KotlinGenerator.generate(parent.convertToGenerator() as Element)
        assertTrue(kotlin.contains("_embedded = generator.generate"), kotlin)
        assertTrue(kotlin.contains("listOf(\"_embedded\")"), kotlin)
    }
}
