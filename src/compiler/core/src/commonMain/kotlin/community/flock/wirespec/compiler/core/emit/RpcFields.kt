package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Part
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Shape
import community.flock.wirespec.compiler.core.parse.ast.Spread
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union

/**
 * A field whose type is an rpc is a function of its type: it takes the parameters of the rpc and gives its result. It
 * holds no data, so it is left out for every emitter that writes models, see [Emitter.emitsRpcFields].
 */
public fun AST.withoutRpcFields(): AST = rpcNames().let { rpcs ->
    copy(
        modules = modules.map { module ->
            module.copy(
                statements = module.statements.map { definition ->
                    when (definition) {
                        is Type -> definition.copy(shape = definition.shape.without(rpcs))
                        is Part -> definition.copy(shape = definition.shape.without(rpcs))
                        is Endpoint, is Channel, is Rpc, is Enum, is Union, is Refined -> definition
                    }
                },
            )
        },
    )
}

/** Whether a field refers to one of the rpcs named [rpcs], which makes it a function. */
public fun Field.isRpcField(rpcs: Set<String>): Boolean = (reference as? Reference.Custom)?.value in rpcs

public fun AST.rpcNames(): Set<String> = modules.toList().flatMap { it.statements }.filterIsInstance<Rpc>().map { it.identifier.value }.toSet()

/** A resolved spread holds the fields of its part, so the functions of the part are left out there too. */
private fun Shape.without(rpcs: Set<String>): Shape = Shape(
    value.mapNotNull { entry ->
        when (entry) {
            is Field -> entry.takeUnless { it.isRpcField(rpcs) }
            is Spread -> entry.copy(fields = entry.fields?.filterNot { it.isRpcField(rpcs) })
        }
    },
)
