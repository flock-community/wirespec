package community.flock.wirespec.ir.core

fun Struct.qualifyNestedRefs(nestedNames: Set<String>): Struct {
    val qualifiedFields = fields.map { field ->
        val typeName = (field.type as? Type.Custom)?.name
        if (typeName != null && typeName in nestedNames) {
            field.copy(type = Type.Custom("${name.pascalCase()}$typeName"))
        } else {
            field
        }
    }
    val qualifiedConstructors = constructors.map { c ->
        c.copy(
            body = c.body.map { stmt ->
                if (stmt is Assignment) {
                    val value = stmt.value
                    if (value is ConstructorStatement) {
                        val typeName = (value.type as? Type.Custom)?.name
                        if (typeName != null && typeName in nestedNames) {
                            Assignment(stmt.name, value.copy(type = Type.Custom("${name.pascalCase()}$typeName")))
                        } else {
                            stmt
                        }
                    } else {
                        stmt
                    }
                } else {
                    stmt
                }
            },
        )
    }
    return copy(
        fields = qualifiedFields,
        constructors = qualifiedConstructors,
        elements = elements.filter { it !is Struct },
    )
}

fun Namespace.flattenNestedStructs(): Namespace {
    val newElements = mutableListOf<Element>()
    for (element in elements) {
        when (element) {
            is Struct -> {
                val nested = element.elements.filterIsInstance<Struct>()
                if (nested.isNotEmpty()) {
                    val nestedNames = nested.map { it.name.pascalCase() }.toSet()
                    for (nestedStruct in nested) {
                        newElements.add(nestedStruct.copy(name = Name.of("${element.name.pascalCase()}${nestedStruct.name.pascalCase()}")))
                    }
                    newElements.add(element.qualifyNestedRefs(nestedNames))
                } else {
                    newElements.add(element)
                }
            }
            else -> newElements.add(element)
        }
    }
    return copy(elements = newElements)
}
