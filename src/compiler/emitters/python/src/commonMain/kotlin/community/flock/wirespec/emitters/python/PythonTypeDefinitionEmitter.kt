package community.flock.wirespec.emitters.python

import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.TypeDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type

interface PythonTypeDefinitionEmitter : TypeDefinitionEmitter, PythonIdentifierEmitter {

    override fun emit(type: Type, module: Module): String =
        if (type.shape.value.isEmpty()) """
            |@dataclass
            |class ${emit(type.identifier)}:
            |${Spacer}pass
            |
            |${type.importReferences().joinToString("\n") { it.emitReferenceCustomImports() }}
            |
        """.trimMargin()
        else """
            |@dataclass
            |class ${emit(type.identifier)}:
            |${type.shape.emit()}
            |
            |${type.importReferences().joinToString("\n") { it.emitReferenceCustomImports() }}
        """.trimMargin()

    override fun Type.Shape.emit() = value.joinToString("\n") { "${Spacer}${it.emit()}" }

    override fun Field.emit() = "${emit(identifier)}: '${reference.emit()}'"

    override fun Reference.emit() = emitType().let { if (isNullable) "Optional[$it]" else it }

    fun Reference.emitType(): String = when (this) {
        is Reference.Dict -> "Dict[str, ${reference.emit()}]"
        is Reference.Iterable -> "List[${reference.emit()}]"
        is Reference.Unit -> "None"
        is Reference.Any -> "Any"
        is Reference.Custom -> value
        is Reference.Primitive -> this.type.emit()
    }

    private fun Reference.emitRoot(): String = when (this) {
        is Reference.Dict -> reference.emitRoot()
        is Reference.Iterable -> reference.emitRoot()
        is Reference.Any -> emitType()
        is Reference.Custom -> emitType()
        is Reference.Primitive -> emitType()
        is Reference.Unit -> emitType()
    }

    fun Reference.Primitive.Type.emit() = when (this) {
        is Reference.Primitive.Type.String -> "str"
        is Reference.Primitive.Type.Integer -> "int"
        is Reference.Primitive.Type.Number -> "float"
        is Reference.Primitive.Type.Boolean -> "bool"
        is Reference.Primitive.Type.Bytes -> "bytes"
    }


    override fun Reference.Primitive.Type.Constraint.emit() = when (this) {
        is Reference.Primitive.Type.Constraint.RegExp -> """${Spacer}bool(re.match(r"$value", self.value))"""
        is Reference.Primitive.Type.Constraint.Bound -> """${Spacer}$min < record.value < $max;"""
    }

    fun Reference.Custom.emitReferenceCustomImports() = "from ..model.${value} import $value"

}
