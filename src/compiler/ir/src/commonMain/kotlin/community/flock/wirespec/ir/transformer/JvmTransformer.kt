package community.flock.wirespec.ir.transformer

import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.Function
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.transform

fun <E : Element> E.toGetterAccessors(
    renameFunction: (Name) -> Name? = { null },
): E = transform {
    matchingElements { iface: Interface ->
        val fieldGetters = iface.fields.map { f ->
            Function(
                name = Name.of("get" + f.name.pascalCase()),
                parameters = emptyList(),
                returnType = f.type,
                body = emptyList(),
            )
        }
        val renamedElements = iface.elements.map { el ->
            if (el is Function) renameFunction(el.name)?.let { el.copy(name = it) } ?: el else el
        }
        iface.copy(
            elements = fieldGetters + renamedElements,
            fields = emptyList(),
        )
    }
}
