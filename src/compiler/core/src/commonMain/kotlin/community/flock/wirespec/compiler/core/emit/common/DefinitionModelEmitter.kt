package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type

interface DefinitionModelEmitter {

    fun Type.Shape.emit(): String

    fun Type.Shape.Field.emit(): String

    fun Type.Shape.Field.Identifier.emit(): String

    fun Type.Shape.Field.Reference.emit(): String

    fun Refined.Validator.emit(): String
}
