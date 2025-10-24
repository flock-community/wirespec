package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union

fun Definition.importReferences(): List<Reference.Custom> = when (this) {
    is Endpoint -> listOf(
        path.filterIsInstance<Endpoint.Segment.Param>().map { it.reference },
        headers.map { it.reference },
        queries.map { it.reference },
        requests.map { it.content?.reference },
        responses.flatMap { listOf(it.content?.reference) + it.headers.map { header -> header.reference } },
    ).flatten().filterNotNull().map { it.flattenListDict() }.filterIsInstance<Reference.Custom>().distinct()

    is Type ->
        shape.value
            .filter { identifier.value != it.reference.root().value }
            .map { it.reference.flattenListDict() }
            .filterIsInstance<Reference.Custom>()
            .distinct()
    is Union -> entries.filterIsInstance<Reference.Custom>()
    is Channel -> if (reference is Reference.Custom) listOf(reference) else emptyList()
    is Enum -> emptyList()
    is Refined -> emptyList()
}
