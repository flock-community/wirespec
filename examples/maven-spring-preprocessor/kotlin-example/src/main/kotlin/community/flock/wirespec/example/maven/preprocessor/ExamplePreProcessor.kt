package community.flock.wirespec.example.maven.preprocessor

class ExamplePreProcessor : (String) -> String {
    override fun invoke(input: String): String = input
}

