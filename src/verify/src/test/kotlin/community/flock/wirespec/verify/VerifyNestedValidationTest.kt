package community.flock.wirespec.verify

import community.flock.wirespec.compiler.core.ir.BinaryOp
import community.flock.wirespec.compiler.core.ir.ConstructorStatement
import community.flock.wirespec.compiler.core.ir.Literal
import community.flock.wirespec.compiler.core.ir.LiteralList
import community.flock.wirespec.compiler.core.ir.LiteralMap
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.NullableEmpty
import community.flock.wirespec.compiler.core.ir.NullableOf
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.file
import community.flock.wirespec.compiler.test.CompileNestedValidationTest
import io.kotest.core.spec.style.FunSpec

class VerifyNestedValidationTest : FunSpec({

    languages.values.forEach { lang ->
        test("nested validation valid - $lang") {
            val testFile = file("NestedValidationValid") {
                import("community.flock.wirespec.generated.model", "Tag")
                import("community.flock.wirespec.generated.model", "Score")
                import("community.flock.wirespec.generated.model", "Label")
                import("community.flock.wirespec.generated.model", "Board")
                main {
                    assign("slotTag", construct(type("Tag")) { arg("value", literal("slot")) })
                    assign("gridLow", construct(type("Score")) { arg("value", literal(0L)) })
                    assign("gridHigh", construct(type("Score")) { arg("value", literal(100L)) })
                    assign("keyTag", construct(type("Tag")) { arg("value", literal("key")) })
                    assign("keyScore", construct(type("Score")) { arg("value", literal(50L)) })
                    assign("gridLabelName", construct(type("Tag")) { arg("value", literal("grid")) })
                    assign("keyLabelName", construct(type("Tag")) { arg("value", literal("keyed")) })
                    assign("board", construct(type("Board")) {
                        arg("owner", NullableEmpty)
                        arg("tags", NullableEmpty)
                        arg("slots", NullableOf(listOf(listOf(NullableOf(VariableReference("slotTag")), NullableEmpty), type("Tag").nullable())))
                        arg("grid", listOf(listOf(listOf(listOf(VariableReference("gridLow"), VariableReference("gridHigh")), type("Score"))), list(type("Score"))))
                        arg("tagsByKey", LiteralMap(mapOf("a" to VariableReference("keyTag")), string, type("Tag")))
                        arg("scoresByKey", LiteralMap(mapOf("x" to listOf(listOf(VariableReference("keyScore")), type("Score"))), string, list(type("Score"))))
                        arg("label", NullableEmpty)
                        arg("labels", listOf(listOf(listOf(listOf(label("gridLabelName", "ABC", "grid")), type("Label"))), list(type("Label"))))
                        arg("labelsByKey", LiteralMap(mapOf("k" to label("keyLabelName", "XYZ", "keyed")), string, type("Label")))
                        arg("limit", literal(10L))
                        arg("ratios", listOf(listOf(literal(0.0), literal(1.0)), number))
                    })
                    assign("errors", functionCall("validate", receiver = VariableReference("board")))
                    assign("expected", literalList(string))
                    assertThat(BinaryOp(VariableReference("errors"), BinaryOp.Operator.EQUALS, VariableReference("expected")), "Valid board should have no validation errors")
                }
            }

            lang.start(name = "nested-validation-valid", fixture = CompileNestedValidationTest)
            lang.run(testFile)
        }

        test("nested validation invalid - $lang") {
            val testFile = file("NestedValidationInvalid") {
                import("community.flock.wirespec.generated.model", "Tag")
                import("community.flock.wirespec.generated.model", "Score")
                import("community.flock.wirespec.generated.model", "Label")
                import("community.flock.wirespec.generated.model", "Board")
                main {
                    assign("ownerTag", construct(type("Tag")) { arg("value", literal("Owner")) })
                    assign("tagValid", construct(type("Tag")) { arg("value", literal("valid")) })
                    assign("tagInvalid", construct(type("Tag")) { arg("value", literal("In valid")) })
                    assign("slotValid", construct(type("Tag")) { arg("value", literal("slot")) })
                    assign("slotInvalid", construct(type("Tag")) { arg("value", literal("SLOT")) })
                    assign("gridFirst", construct(type("Score")) { arg("value", literal(1L)) })
                    assign("gridSecond", construct(type("Score")) { arg("value", literal(2L)) })
                    assign("gridInvalid", construct(type("Score")) { arg("value", literal(101L)) })
                    assign("keyValid", construct(type("Tag")) { arg("value", literal("valid")) })
                    assign("keyInvalid", construct(type("Tag")) { arg("value", literal("in-valid")) })
                    assign("scoreValid", construct(type("Score")) { arg("value", literal(1L)) })
                    assign("scoreInvalid", construct(type("Score")) { arg("value", literal(-1L)) })
                    assign("labelName", construct(type("Tag")) { arg("value", literal("label")) })
                    assign("gridLabelValidName", construct(type("Tag")) { arg("value", literal("valid")) })
                    assign("gridLabelInvalidName", construct(type("Tag")) { arg("value", literal("Invalid")) })
                    assign("keyLabelName", construct(type("Tag")) { arg("value", literal("keyed")) })
                    assign("board", construct(type("Board")) {
                        arg("owner", NullableOf(VariableReference("ownerTag")))
                        arg("tags", NullableOf(listOf(listOf(VariableReference("tagValid"), VariableReference("tagInvalid")), type("Tag"))))
                        arg("slots", NullableOf(listOf(listOf(NullableOf(VariableReference("slotValid")), NullableEmpty, NullableOf(VariableReference("slotInvalid"))), type("Tag").nullable())))
                        arg(
                            "grid",
                            listOf(
                                listOf(
                                    listOf(listOf(VariableReference("gridFirst"), VariableReference("gridSecond")), type("Score")),
                                    listOf(listOf(VariableReference("gridInvalid")), type("Score")),
                                ),
                                list(type("Score")),
                            ),
                        )
                        arg("tagsByKey", LiteralMap(mapOf("a" to VariableReference("keyValid"), "b" to VariableReference("keyInvalid")), string, type("Tag")))
                        arg("scoresByKey", LiteralMap(mapOf("x" to listOf(listOf(VariableReference("scoreValid"), VariableReference("scoreInvalid")), type("Score"))), string, list(type("Score"))))
                        arg("label", NullableOf(label("labelName", "abc", "label")))
                        arg(
                            "labels",
                            listOf(
                                listOf(listOf(listOf(label("gridLabelValidName", "ABC", "fine"), label("gridLabelInvalidName", "ABC", "fine")), type("Label"))),
                                list(type("Label")),
                            ),
                        )
                        arg("labelsByKey", LiteralMap(mapOf("k" to label("keyLabelName", "ABCD", "keyed", "Not keyed")), string, type("Label")))
                        arg("limit", literal(11L))
                        arg("ratios", listOf(listOf(literal(0.5), literal(1.5)), number))
                    })
                    assign("errors", functionCall("validate", receiver = VariableReference("board")))
                    assign(
                        "expected",
                        literalList(
                            listOf(
                                literal("owner"),
                                literal("tags[1]"),
                                literal("slots[2]"),
                                literal("grid[1][0]"),
                                literal("tagsByKey[b]"),
                                literal("scoresByKey[x][1]"),
                                literal("label.code"),
                                literal("labels[0][1].name"),
                                literal("labelsByKey[k].code"),
                                literal("labelsByKey[k].aliases[1]"),
                                literal("limit"),
                                literal("ratios[1]"),
                            ),
                            string,
                        ),
                    )
                    assertThat(BinaryOp(VariableReference("errors"), BinaryOp.Operator.EQUALS, VariableReference("expected")), "Every nested invalid value should be reported by its path")
                }
            }

            lang.start(name = "nested-validation-invalid", fixture = CompileNestedValidationTest)
            lang.run(testFile)
        }

        test("recursive validation - $lang") {
            val testFile = file("RecursiveValidation") {
                import("community.flock.wirespec.generated.model", "Tag")
                import("community.flock.wirespec.generated.model", "TreeNode")
                main {
                    assign("rootTag", construct(type("Tag")) { arg("value", literal("root")) })
                    assign("childTag", construct(type("Tag")) { arg("value", literal("child")) })
                    assign("validLeafTag", construct(type("Tag")) { arg("value", literal("leaf")) })
                    assign("invalidLeafTag", construct(type("Tag")) { arg("value", literal("Leaf")) })
                    assign("validLeaf", construct(type("TreeNode")) {
                        arg("tag", VariableReference("validLeafTag"))
                        arg("children", emptyList(type("TreeNode")))
                    })
                    assign("invalidLeaf", construct(type("TreeNode")) {
                        arg("tag", VariableReference("invalidLeafTag"))
                        arg("children", emptyList(type("TreeNode")))
                    })
                    assign("child", construct(type("TreeNode")) {
                        arg("tag", VariableReference("childTag"))
                        arg("children", listOf(listOf(VariableReference("validLeaf"), VariableReference("invalidLeaf")), type("TreeNode")))
                    })
                    assign("root", construct(type("TreeNode")) {
                        arg("tag", VariableReference("rootTag"))
                        arg("children", listOf(listOf(VariableReference("child")), type("TreeNode")))
                    })
                    assign("errors", functionCall("validate", receiver = VariableReference("root")))
                    assign("expected", literalList(listOf(literal("children[0].children[1].tag")), string))
                    assertThat(BinaryOp(VariableReference("errors"), BinaryOp.Operator.EQUALS, VariableReference("expected")), "An invalid tag deep in the tree should be reported by its path")
                }
            }

            lang.start(name = "recursive-validation", fixture = CompileNestedValidationTest)
            lang.run(testFile)
        }
    }
})

private fun label(nameVariable: String, code: String, vararg aliases: String) = ConstructorStatement(
    type = Type.Custom("Label"),
    namedArguments = mapOf(
        Name.of("name") to VariableReference(nameVariable),
        Name.of("code") to Literal(code, Type.String),
        Name.of("aliases") to LiteralList(aliases.map { Literal(it, Type.String) }, Type.String),
    ),
)
