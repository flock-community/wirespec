// Adds `resolveAllDependencies` to every project: downloads the artifacts of every resolvable
// configuration, so a fresh GRADLE_USER_HOME ends up holding everything the build can need.
// Used by .github/workflows/gradle-dep-cache.yml to build the read-only dependency cache.
allprojects {
    tasks.register("resolveAllDependencies") {
        val resolvable = configurations.filter { it.isCanBeResolved }
        doLast {
            resolvable.forEach { configuration ->
                val artifacts = configuration.incoming.artifactView { isLenient = true }.artifacts
                artifacts.artifactFiles.files
                artifacts.failures.forEach { logger.warn("${configuration.name}: ${it.message}") }
            }
        }
    }
}
