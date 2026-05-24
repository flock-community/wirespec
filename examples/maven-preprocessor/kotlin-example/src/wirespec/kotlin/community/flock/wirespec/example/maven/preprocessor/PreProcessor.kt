package community.flock.wirespec.example.maven.preprocessor

import community.flock.kotlinx.openapi.bindings.OpenAPIV3
import community.flock.kotlinx.openapi.bindings.OpenAPIV30Model

class PreProcessor : (String) -> String {
    override fun invoke(input: String) =
        (OpenAPIV3.decodeFromString(input) as OpenAPIV30Model)
            .run { copy(paths = paths
                ?.filterKeys { it.value == "/pet" }
                ?.mapValues { it.value.copy(
                    put = it.value.put?.copy(operationId = it.value.put?.operationId?.plus("Processed")),
                    post = it.value.post?.copy(operationId = it.value.post?.operationId?.plus("Processed")),
                ) }
            ) }
            .let(OpenAPIV3::encodeToString)

    }