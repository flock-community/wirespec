package community.flock.wirespec.example.maven.preprocessor

import community.flock.kotlinx.openapi.bindings.v3.OpenAPI

class PreProcessor : (String) -> String {
    override fun invoke(input: String) = OpenAPI.Default.decodeFromString(input)
        .run {
            copy(
                paths = paths
                    .filterKeys { it.value == "/pet" }
                    .mapValues {
                        it.value.copy(
                            put = it.value.put?.copy(operationId = it.value.put?.operationId?.plus("Processed")),
                            post = it.value.post?.copy(operationId = it.value.post?.operationId?.plus("Processed")),
                        )
                    },
            )
        }
        .let(OpenAPI.Default::encodeToString)
}
