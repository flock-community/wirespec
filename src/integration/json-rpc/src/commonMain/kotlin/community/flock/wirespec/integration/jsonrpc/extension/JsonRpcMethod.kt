package community.flock.wirespec.integration.jsonrpc.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.Function
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Interface
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Namespace
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.converter.convert
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.fields
import community.flock.wirespec.compiler.core.ir.File as LanguageFile

/** Writes the JSON-RPC client and server of the rpcs for one language. */
internal interface JsonRpcDialect {
    fun extend(ir: IR, packageName: PackageName, methods: List<JsonRpcMethod>): IR
}

/**
 * An rpc as a JSON-RPC method: its [name] on the wire, and the `Service` function the emitter wrote for it, whose
 * parameters and result type the client and the server take as that language writes them.
 */
internal class JsonRpcMethod(val rpc: Rpc, val service: Function) {
    val name: String = rpc.annotations.methodName() ?: Name.of(rpc.identifier.value).camelCase()

    /** The name of the rpc, and of its namespace, in the generated code. */
    val rpcName: String = Name.of(rpc.identifier.value).pascalCase()

    /** A Rust or Python function takes its receiver as a parameter of its own, which is not sent. */
    val params: List<JsonRpcParam> = rpc.shape.fields
        .zip(service.parameters.filterNot { it.name.value() in RECEIVERS })
        .mapIndexed { position, (field, parameter) -> JsonRpcParam(field.identifier.value, parameter.name, parameter.type, field.reference, position) }

    val result: Reference = rpc.result

    val error: Reference? = rpc.error

    val isAsync: Boolean = service.isAsync

    /** The models the params, the result and the error of the rpc refer to. */
    val models: List<String> = (rpc.shape.fields.map { it.reference } + listOfNotNull(result, error))
        .flatMap { it.customNames() }
        .distinct()

    private fun List<Annotation>.methodName(): String? = find { it.name == JsonRpcExtension.METHOD_ANNOTATION }
        ?.parameters?.firstOrNull()?.value
        ?.let { it as? Annotation.Value.Single }
        ?.value

    private fun Reference.customNames(): List<String> = when (this) {
        is Reference.Custom -> listOf(value)
        is Reference.Iterable -> reference.customNames()
        is Reference.Dict -> reference.customNames()
        is Reference.Any, is Reference.Unit, is Reference.Primitive -> emptyList()
    }

    private companion object {
        val RECEIVERS = setOf("self", "&self")
    }
}

/**
 * A param of a method: its [wireName] in the params object, and the [name] and [type] of the parameter of the
 * `Service` function. The [reference] is the type in the contract, from which a value is read and written.
 */
internal class JsonRpcParam(val wireName: String, val name: Name, val type: Type, val reference: Reference, val position: Int) {
    val isNullable: Boolean = reference.isNullable
}

/** The type of a value in the generated code, as the IR converts it; a nullable value is read and written as what it holds. */
internal fun Reference.valueType(): Type = copy(isNullable = false).convert()

/** A body of type String is text, not JSON, to the Wirespec serializations, so the generated code writes a String value as JSON itself. */
internal fun Reference.isString(): Boolean = this is Reference.Primitive && type is Reference.Primitive.Type.String

/** The `Service` function the emitter wrote for [rpc], in the namespace named after it. */
internal fun IR.serviceOf(rpc: Rpc): Function = filterIsInstance<LanguageFile>()
    .flatMap { it.elements }
    .filterIsInstance<Namespace>()
    .filter { it.name.pascalCase() == Name.of(rpc.identifier.value).pascalCase() }
    .firstNotNullOfOrNull { namespace ->
        namespace.elements.filterIsInstance<Interface>()
            .find { it.name.pascalCase() == "Service" }
            ?.elements?.filterIsInstance<Function>()?.singleOrNull()
    }
    ?: error("The emitter wrote no Service for rpc ${rpc.identifier.value}")
