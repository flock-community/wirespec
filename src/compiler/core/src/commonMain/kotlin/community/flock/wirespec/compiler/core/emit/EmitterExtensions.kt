package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Part
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.core.parse.ast.fields

public fun Definition.importReferences(): List<Reference.Custom> = when (this) {
    is Endpoint -> sequenceOf(
        path.filterIsInstance<Endpoint.Segment.Param>().map { it.reference },
        headers.fields.map { it.reference },
        queries.fields.map { it.reference },
        requests.map { it.content?.reference },
        responses.flatMap { listOf(it.content?.reference) + it.headers.fields.map { header -> header.reference } },
    ).flatten().mapNotNull { it?.flatten() }.filterIsInstance<Reference.Custom>().distinct().toList()

    is Type ->
        shape.value.fields
            .filter { identifier.value != it.reference.root().value }
            .map { it.reference.flatten() }
            .filterIsInstance<Reference.Custom>()
            .distinct()
    is Union -> entries.filterIsInstance<Reference.Custom>()
    is Channel -> if (reference is Reference.Custom) listOf(reference) else emptyList()
    is Rpc -> (shape.value.fields.map { it.reference } + result + listOfNotNull(error))
        .map { it.flatten() }
        .filterIsInstance<Reference.Custom>()
        .distinct()
    is Part -> shape.value.fields.map { it.reference.flatten() }.filterIsInstance<Reference.Custom>().distinct()
    is Enum -> emptyList()
    is Refined -> emptyList()
}
