package community.flock.wirespec.openapi.common

import arrow.core.nonEmptyListOf
import community.flock.kotlinx.openapi.bindings.OpenAPIModel
import community.flock.kotlinx.openapi.bindings.OpenAPIV2Reference
import community.flock.kotlinx.openapi.bindings.OpenAPIV3Reference
import community.flock.kotlinx.openapi.bindings.Operation
import community.flock.kotlinx.openapi.bindings.Path
import community.flock.kotlinx.openapi.bindings.PathItem
import community.flock.kotlinx.openapi.bindings.ResponseOrReference
import community.flock.kotlinx.openapi.bindings.StatusCode
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Statements
import kotlinx.serialization.json.Json
import community.flock.kotlinx.openapi.bindings.Reference as OpenAPIReference

internal fun jsonDefault(strict: Boolean) = Json {
    prettyPrint = true
    ignoreUnknownKeys = !strict
}

internal fun parseOpenApi(moduleContent: ModuleContent, openApiParser: (String) -> Statements) = AST(
    modules = nonEmptyListOf(
        Module(
            fileUri = moduleContent.fileUri,
            statements = openApiParser(moduleContent.content),
        ),
    ),
)

internal fun PathItem.toOperationList() = Endpoint.Method.entries
    .associateWith {
        when (it) {
            Endpoint.Method.GET -> get
            Endpoint.Method.POST -> post
            Endpoint.Method.PUT -> put
            Endpoint.Method.DELETE -> delete
            Endpoint.Method.OPTIONS -> options
            Endpoint.Method.HEAD -> head
            Endpoint.Method.PATCH -> patch
            Endpoint.Method.TRACE -> trace
        }
    }
    .filterNotNullValues()

internal fun String.isParam() = this[0] == '{' && this[length - 1] == '}'

internal fun OpenAPIModel.flatMapRequests(f: FlattenRequest.() -> List<Definition>) = paths
    .flatMap { (path, pathItem) ->
        pathItem.toOperationList()
            .map { (method, operation) ->
                FlattenRequest(path = path, pathItem = pathItem, method = method, operation = operation)
            }
    }
    .flatMap(f)

internal data class FlattenRequest(
    val path: Path,
    val pathItem: PathItem,
    val method: Endpoint.Method,
    val operation: Operation,
)

internal fun OpenAPIModel.flatMapResponses(f: FlattenResponse.() -> List<Definition>) = paths
    .flatMap { (path, pathItem) ->
        pathItem.toOperationList()
            .flatMap { (method, operation) ->
                operation.responses.orEmpty()
                    .map { (statusCode, response) ->
                        FlattenResponse(
                            path = path,
                            pathItem = pathItem,
                            method = method,
                            operation = operation,
                            statusCode = statusCode,
                            response = response,
                        )
                    }
            }
    }
    .flatMap(f)

internal data class FlattenResponse(
    val path: Path,
    val pathItem: PathItem,
    val method: Endpoint.Method,
    val operation: Operation,
    val statusCode: StatusCode,
    val response: ResponseOrReference,
)

internal fun Path.toName(): String = value
    .split("/")
    .drop(1)
    .filter { it.isNotBlank() }
    .joinToString("") {
        when (it.isParam()) {
            true -> className(it.substring(1, it.length - 1))
            false -> className(it)
        }
    }

internal fun Reference.toIterable(isNullable: Boolean) = Reference.Iterable(reference = this, isNullable = isNullable)

internal fun Reference.toDict(isNullable: Boolean) = Reference.Dict(reference = this, isNullable = isNullable)

internal fun OpenAPIReference.getReference() = ref.value.split("/").let {
    when (this) {
        is OpenAPIV2Reference -> it.getOrNull(2)
        is OpenAPIV3Reference -> it.getOrNull(3)
    }
} ?: error("Wrong reference: ${ref.value}")

internal fun Operation.toName() = operationId?.let { className(it) }

internal fun String.sanitize() = this
    .split(".", " ", "-")
    .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
    .joinToString("")
    .asSequence()
    .filter { it.isLetterOrDigit() || it in listOf('_') }
    .joinToString("")
