# Wirespec examples

Each directory is a self-contained example project showing Wirespec with a particular build tool or
integration (Maven, Gradle, npm, Cargo, sbt).

All examples are driven by the Gradle build in this directory. Examples are discovered by their
marker file (`pom.xml`, `settings.gradle.kts`, `package.json`, `Cargo.toml`, `build.sbt`), so adding
a new example directory automatically adds it to the build.

```shell
./gradlew build             # build and test all examples
./gradlew clean             # clean all examples
./gradlew format            # format all examples
./gradlew yolo              # build Maven and Gradle examples without running tests
./gradlew installWrappers   # (re)install the Maven and Gradle wrappers into every example
./gradlew tasks             # list per-example tasks, e.g. buildMavenSpringCompile
```

Most examples resolve Wirespec artifacts with version `0.0.0-SNAPSHOT` from Maven local; publish
them first from the repository root:

```shell
./gradlew publishToMavenLocal
```

The npm example additionally needs the npm plugin distribution:

```shell
./gradlew :src:plugin:npm:jsNodeProductionLibraryDistribution
```

## Wrappers

The wrappers checked into each example are installed by `./gradlew installWrappers`, never edited by
hand. The Maven wrapper comes from the single template in `maven/wrapper`; the Gradle wrapper
scripts and jar come from this build's own wrapper, while each example keeps its own
`gradle-wrapper.properties` to pin its Gradle version. To upgrade this build's wrapper (and thereby
the examples'), run `./gradlew wrapper --gradle-version <version>` here, then `installWrappers`.
