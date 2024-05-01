package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type

interface DefinitionModelEmitter {

    fun Type.Shape.emit(): String

    fun Field.emit(): String

    fun Field.Identifier.emit(): String

    fun Field.Reference.emit(): String

    fun Refined.Validator.emit(): String
}
