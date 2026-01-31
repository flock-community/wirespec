package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.EnumDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.language.converter.convert
import community.flock.wirespec.language.core.generator.generateJava

interface JavaEnumDefinitionEmitter : EnumDefinitionEmitter, JavaIdentifierEmitter {

    override fun emit(enum: Enum, module: Module) = enum
        .convert()
        .run {
            copy(
                entries = entries.map {
                    community.flock.wirespec.language.core.Enum.Entry(it.name.sanitizeEnum(), listOf("\"${it.name}\""))
                },
                fields = listOf(
                    community.flock.wirespec.language.core.Field(
                        "label",
                        community.flock.wirespec.language.core.Type.String
                    )
                ),
                constructors = listOf(
                    community.flock.wirespec.language.core.Constructor(
                        listOf(
                            community.flock.wirespec.language.core.Parameter(
                                "label",
                                community.flock.wirespec.language.core.Type.String
                            )
                        ),
                        listOf(
                            community.flock.wirespec.language.core.Assignment(
                                "this.label",
                                community.flock.wirespec.language.core.RawExpression("label"),
                                true
                            )
                        )
                    )
                ),
                elements = listOf(
                    community.flock.wirespec.language.core.Function(
                        "toString", emptyList(), community.flock.wirespec.language.core.Type.String, listOf(
                            community.flock.wirespec.language.core.ReturnStatement(
                                community.flock.wirespec.language.core.RawExpression(
                                    "label"
                                )
                            )
                        )
                    ),
                    community.flock.wirespec.language.core.Function(
                        "getLabel", emptyList(), community.flock.wirespec.language.core.Type.String, listOf(
                            community.flock.wirespec.language.core.ReturnStatement(
                                community.flock.wirespec.language.core.RawExpression(
                                    "label"
                                )
                            )
                        )
                    )
                )
            )
        }
        .generateJava()
        .run { this + "\n" }

    fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//")
        .joinToString("_")
        .sanitizeFirstIsDigit()
        .sanitizeKeywords()

}
