package community.flock.wirespec.compiler.core.ir.transformer

import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.Function
import community.flock.wirespec.compiler.core.ir.Interface
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.transform

public fun <E : Element> E.toGetterAccessors(
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
