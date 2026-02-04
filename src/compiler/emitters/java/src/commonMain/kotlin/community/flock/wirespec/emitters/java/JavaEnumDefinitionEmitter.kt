package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.EnumDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.language.converter.convert
import community.flock.wirespec.language.core.Assignment
import community.flock.wirespec.language.core.Constructor
import community.flock.wirespec.language.core.Field
import community.flock.wirespec.language.core.RawExpression
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.function
import community.flock.wirespec.language.generator.generateJava
import community.flock.wirespec.language.core.Enum as LanguageEnum

interface JavaEnumDefinitionEmitter : EnumDefinitionEmitter, JavaIdentifierEmitter {

    override fun emit(enum: Enum, module: Module) = enum
        .convert()
        .run {
            copy(
                entries = entries.map {
                    LanguageEnum.Entry(it.name.sanitizeEnum(), listOf("\"${it.name}\""))
                },
                fields = listOf(
                    Field("label", Type.String),
                ),
                constructors = listOf(
                    Constructor(
                        parameters = listOf(community.flock.wirespec.language.core.Parameter("label", Type.String)),
                        body = listOf(Assignment("this.label", RawExpression("label"), true)),
                    ),
                ),
                elements = listOf(
                    function("toString", Type.String, isOverride = true) {
                        returns(RawExpression("label"))
                    },
                    function("getLabel", Type.String) {
                        returns(RawExpression("label"))
                    },
                ),
            )
        }
        .generateJava()
        .run { this + "\n" }

    fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//")
        .joinToString("_")
        .sanitizeFirstIsDigit()
        .sanitizeKeywords()

}
