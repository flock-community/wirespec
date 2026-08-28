# Wirespec examples

Each directory is a self-contained example project showing Wirespec with a particular build tool or
integration (Maven, Gradle, npm, Cargo, sbt).

All examples are modules of the root Gradle build, discovered by their marker file (`pom.xml`,
`package.json`, `Cargo.toml`, `build.sbt`), so adding a new example directory automatically adds it
to the build. The two Gradle examples keep their own standalone builds and are driven through
nested builds. Examples are never built by the default build — only by the tasks below.

From the repository root:

```shell
./gradlew buildExamples     # build and test all examples
./gradlew cleanExamples     # clean all examples
./gradlew formatExamples    # format all examples
./gradlew yoloExamples      # build Maven and Gradle examples without running tests
./gradlew installWrappers   # install the sbt wrapper and the cargo toolchain where missing
```

Single examples all share one task shape, regardless of build tool:

```shell
./gradlew :examples:build-maven-spring-compile
./gradlew :examples:build-gradle-ktor
./gradlew :examples:clean-npm-typescript
```

Example builds are self-contained: each task provisions what it needs, and an example declaring the
GraalVM `native-maven-plugin` automatically builds native when a native toolchain (`GRAALVM_HOME`
or `native-image` on the `PATH`) is available, and falls back to a plain JVM build otherwise.

Most examples resolve Wirespec artifacts with version `0.0.0-SNAPSHOT` from Maven local; publish
them first from the repository root:

```shell
./gradlew publishToMavenLocal
```

The npm example additionally needs the npm plugin distribution:

```shell
./gradlew :src:plugin:npm:jsNodeProductionLibraryDistribution
```

## Wrappers and toolchains

The example projects carry no wrappers of their own; Gradle is responsible for installing and
running the right tools:

- **Maven**: the Maven wrapper is never checked in — Gradle downloads it fresh from Maven Central
  into `examples/build/maven-wrapper`, at the versions pinned in
  `examples/.mvn/wrapper/maven-wrapper.properties`, and all Maven examples share it.
- **Gradle**: the standalone Gradle examples run as nested builds with the root wrapper's Gradle
  version.
- **sbt**: `installWrappers` downloads the [sbt-extras](https://github.com/dwijnand/sbt-extras)
  wrapper into `scala-zio/sbt`; the sbt version is pinned in `scala-zio/project/build.properties`.
- **cargo**: `installWrappers` installs the toolchain via rustup when `cargo` is not already
  available.
