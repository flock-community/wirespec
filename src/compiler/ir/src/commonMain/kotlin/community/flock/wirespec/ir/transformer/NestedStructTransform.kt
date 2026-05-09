package community.flock.wirespec.ir.transformer

import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.transform

fun Struct.qualifyNestedRefs(nestedNames: Set<String>): Struct {
    val prefix = name.pascalCase()
    return transform {
        matching<Type.Custom> { type ->
            if (type.name in nestedNames) type.copy(name = "$prefix${type.name}") else type
        }
    }.copy(elements = elements.filterNot { it is Struct })
}

fun Namespace.flattenNestedStructs(): Namespace = copy(
    elements = elements.flatMap { element ->
        if (element !is Struct) return@flatMap listOf(element)
        val nested = element.elements.filterIsInstance<Struct>()
        if (nested.isEmpty()) return@flatMap listOf(element)

        val parentPrefix = element.name.pascalCase()
        val nestedNames = nested.mapTo(mutableSetOf()) { it.name.pascalCase() }
        val flattened = nested.map { it.copy(name = Name.of("$parentPrefix${it.name.pascalCase()}")) }
        flattened + element.qualifyNestedRefs(nestedNames)
    },
)
