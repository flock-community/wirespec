package community.flock.wirespec.example.maven.preprocessor

import community.flock.kotlinx.openapi.bindings.v3.OpenAPI

class PreProcessor : (String) -> String {
    override fun invoke(input: String): String {
        return OpenAPI.decodeFromString(input)
            .run { copy(paths = paths
                .filterKeys { it.value == "/pet" }
            ) }
            .let(OpenAPI::encodeToString)

    }
}

