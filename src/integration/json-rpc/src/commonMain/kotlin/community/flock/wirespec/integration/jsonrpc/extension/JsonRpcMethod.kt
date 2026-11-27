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
    /** JSON-RPC reserves the methods that start with `rpc.` for its own extensions. */
    val name: String = (rpc.annotations.single(JsonRpcExtension.METHOD_ANNOTATION) ?: Name.of(rpc.identifier.value).camelCase())
        .also { require(!it.startsWith("rpc.")) { "JSON-RPC reserves the method $it of rpc ${rpc.identifier.value}: a method name may not start with rpc." } }

    /**
     * The code of the error object the rpc answers its own error with. JSON-RPC reserves -32768 to -32000 for its own
     * errors and leaves the other codes to the application, which the error of an rpc is part of.
     */
    val errorCode: Int = rpc.annotations.single(JsonRpcExtension.ERROR_ANNOTATION)
        ?.let { code -> requireNotNull(code.toIntOrNull()) { "The error code $code of rpc ${rpc.identifier.value} is no integer" } }
        ?.also { require(it !in RESERVED_CODES) { "JSON-RPC reserves the error code $it of rpc ${rpc.identifier.value}: use a code outside $RESERVED_CODES" } }
        ?: JsonRpcExtension.ERROR_CODE

    /** The message of that error object: the name of the error type in the contract. */
    val errorMessage: String = (rpc.error as? Reference.Custom)?.value ?: "Error"

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

    private fun List<Annotation>.single(name: String): String? = find { it.name == name }
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
        val RESERVED_CODES = -32768..-32000
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
