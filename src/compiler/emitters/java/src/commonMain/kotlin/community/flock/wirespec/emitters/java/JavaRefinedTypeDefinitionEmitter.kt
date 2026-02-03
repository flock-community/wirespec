package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.RefinedTypeDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.language.converter.convert
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.function
import community.flock.wirespec.language.core.RawExpression
import community.flock.wirespec.language.generator.generateJava

interface JavaRefinedTypeDefinitionEmitter: RefinedTypeDefinitionEmitter, JavaTypeDefinitionEmitter {

    override fun emit(refined: Refined) = refined
        .convert()
        .run {
            copy(
                interfaces= listOf(Type.Custom("Wirespec.Refined")),
                elements = listOf(
                    function("toString", Type.Custom("String"), isOverride = true) {
                        returns(RawExpression("value"))
                    },
                    function("validate", Type.Boolean, isStatic = true) {
                        arg("record", Type.Custom(name))
                        returns(RawExpression(refined.emitValidator().removeSuffix(";")))
                    },
                    function("getValue", Type.Custom("String"), isOverride = true) {
                        returns(RawExpression("value"))
                    }
                )
            )
        }
        .generateJava()
        .run { this + "\n" }

    override fun Refined.emitValidator():String {
        val defaultReturn = "true;"
        return when (val type = reference.type) {
            is Reference.Primitive.Type.Integer -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.Number -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.String -> type.constraint?.emit() ?: defaultReturn
            Reference.Primitive.Type.Boolean -> defaultReturn
            Reference.Primitive.Type.Bytes -> defaultReturn
        }
    }

}
