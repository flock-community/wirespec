# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Wirespec is a modern API design and code generation tool that streamlines contract-driven development. It defines APIs through a human-readable specification language (.ws files) and generates type-safe bindings for multiple target languages (Kotlin, Java, TypeScript, Python). The project also supports bidirectional conversion between Wirespec and other API specification formats (OpenAPI v2/v3, Avro).

## Core Architecture

### Compilation Pipeline

The compiler follows a traditional multi-stage pipeline architecture:

```
Input (.ws file)
    ↓
TOKENIZER (src/compiler/core/.../tokenizer/) - Lexical analysis using regex-based token matching
    ↓
PARSER (src/compiler/core/.../parse/) - Recursive descent parser producing AST
    ↓
VALIDATOR (src/compiler/core/.../validate/) - Schema validation with strict/lenient modes
    ↓
EMITTERS (src/compiler/emitters/{kotlin,java,typescript,python,wirespec}/) - Language-specific code generation
    ↓
Output (language-specific files)
```

Key files:
- `src/compiler/core/src/commonMain/kotlin/community/flock/wirespec/compiler/core/Compiler.kt` - Main compiler orchestration
- `src/compiler/core/src/commonMain/kotlin/community/flock/wirespec/compiler/core/parse/Parser.kt` - AST parser
- `src/compiler/core/src/commonMain/kotlin/community/flock/wirespec/compiler/core/validate/Validator.kt` - Validation logic

### Module Structure

The project is organized into four primary module categories under `/src`:

1. **Compiler** (`src/compiler/`):
   - `core/` - Tokenizer, Parser, Validator, and core emission logic
   - `emitters/{kotlin,java,typescript,python,wirespec}/` - Language-specific code generators
   - `lib/` - Shared compiler utilities
   - `test/` - Test fixtures and utilities

2. **Converters** (`src/converter/`):
   - `openapi/` - Bidirectional OpenAPI v2/v3 conversion
   - `avro/` - Bidirectional Apache Avro schema conversion
   - `common/` - Shared conversion utilities

3. **Plugins** (`src/plugin/`):
   - `cli/` - Command-line interface (Kotlin Native executable)
   - `gradle/` - Gradle plugin for build integration
   - `maven/` - Maven plugin for build integration
   - `npm/` - NPM package distribution
   - `arguments/` - Shared argument parsing

4. **Integration Libraries** (`src/integration/`):
   - `jackson/` - Jackson serialization support
   - `spring/` - Spring Framework integration
   - `avro/` - Avro runtime integration
   - `wirespec/` - Wirespec runtime support

5. **IDE Extensions** (`src/ide/`):
   - `intellij-plugin/` - IntelliJ IDEA plugin
   - `vscode/` - Visual Studio Code extension with Language Server Protocol (LSP)

## Technology Stack

- **Language**: Kotlin Multiplatform (2.0.21/2.1.0)
- **Build**: Gradle with Kotlin DSL
- **Testing**: Kotest 5.9.1 with multiplatform test support
- **Error Handling**: Arrow 1.2.4 (`Either`/`EitherNel` for functional error accumulation)
- **Serialization**: Kotlinx Serialization 1.7.0, Jackson 2.16.1
- **Java**: JDK 17 toolchain (JDK 21 recommended)
- **Compilation Targets**: JVM, macOS (x64/Arm64), Linux x64, Node.js

## Common Development Commands

### Building

```bash
# Complete build including tests and examples
make all

# Build Wirespec compiler and VSCode extension
make build
# OR
./gradlew build && (cd src/ide/vscode && npm i && npm run build)

# Build only (no tests)
./gradlew assemble

# Clean build artifacts
./scripts/clean.sh
```

### Testing

```bash
# Comprehensive test across all platforms (macOS Native, JVM, Node.js, Docker)
make test

# JVM tests only
./gradlew jvmTest
# OR
make jvm

# Individual module tests
./gradlew :src:compiler:core:test
```

The test script (`scripts/test.sh`) validates all compilation artifacts by:
1. Compiling test Wirespec files to all target languages (Java, Kotlin, TypeScript, Python, Wirespec)
2. Running conversion from OpenAPI to all target languages
3. Testing macOS native, JVM jar, Node.js, and Docker artifacts

### Code Quality

```bash
# Format code (runs Spotless)
make format
# OR
./scripts/format.sh

# Verify formatting
./scripts/verify.sh
```

### Running the CLI

```bash
# Compile Wirespec files
wirespec compile -i <input.ws> -l <language> -o <output-dir> [-p <package-name>]

# Convert from OpenAPI
wirespec convert -i <openapi.json> <openapiv2|openapiv3> -l <language> -o <output-dir> [-p <package-name>]

# Supported languages: Java, Kotlin, TypeScript, Python, Wirespec
```

### Examples and Docker

```bash
# Generate example outputs
make example
# OR
./scripts/example.sh

# Build Docker image
make image
# OR
./scripts/image.sh

# Local development setup
./scripts/local.sh
```

### Running Individual Tests

```bash
# Run specific test class
./gradlew :src:compiler:core:test --tests "community.flock.wirespec.compiler.core.parse.ParseTest"

# Run all tests in a module
./gradlew :src:compiler:core:test

# Run tests for specific platform
./gradlew :src:compiler:core:jvmTest
./gradlew :src:compiler:core:jsNodeTest
```

## Code Patterns and Conventions

### Functional Error Handling

The codebase uses Arrow's `Either` and `EitherNel` (Either + NonEmptyList) for error handling:

```kotlin
// Single error
fun parse(): Either<ParseException, AST>

// Accumulated errors
fun validate(): EitherNel<ValidationError, Unit>
```

When working with validation or parsing code, use `Either.Left` for errors and `Either.Right` for success. Use `EitherNel` when multiple errors should be accumulated rather than failing fast.

### Multiplatform Source Sets

Code is organized by compilation target:
- `commonMain/` - Platform-independent code (most compiler logic)
- `commonTest/` - Platform-independent tests
- `jvmMain/`, `jvmTest/` - JVM-specific code/tests
- `jsMain/`, `jsTest/` - JavaScript-specific code/tests
- `nativeMain/`, `nativeTest/` - Native-specific code/tests

When adding features, start in `commonMain` unless platform-specific functionality is required.

### Emitter Pattern

All language emitters implement the `Emitter` interface. Each emitter operates on the AST (represented as `Statements`) to generate language-specific code:

```kotlin
interface Emitter {
    fun emit(statements: List<Statement>): List<String>
}
```

Recent refactoring has standardized emitters to operate over `Statements` instead of individual model types. When modifying emitters, ensure they handle all statement types (endpoints, types, channels, enums, refined types, unions).

### Testing with Kotest

Tests use Kotest's spec-based testing:

```kotlin
class MyTest : StringSpec({
    "test description" {
        // test body
        result shouldBe expected
    }
})
```

Use Kotest matchers (`shouldBe`, `shouldNotBe`, etc.) and Arrow test utilities for `Either` assertions.

## Critical Development Notes

### Emitter Architecture

As of recent commits, emitters have been refactored to operate over `Statements` rather than individual `Model` types. When working on emitters:
- Server emitters should support status-specific response handling
- Type emitters should handle all statement variants
- Headers in TypeScript should maintain type compatibility (case-insensitive handling)
- Avro emitters require special attention to schema mapping

### Case Sensitivity in Headers

TypeScript emitters handle headers with case-insensitive semantics. When modifying header serialization, ensure type compatibility is maintained.

### Gradle Configuration

- Version management is centralized in `gradle/libs.versions.toml`
- Custom plugins are in `gradle/plugins/` (publish-sonatype, spotless)
- All 44 submodules are declared in `settings.gradle.kts`
- Java toolchain is set to JDK 17, but JDK 21 is recommended for development

### VSCode Extension Build

The VSCode extension (`src/ide/vscode/`) uses TypeScript and requires separate build steps:
1. `npm install` to install dependencies
2. `npm run build` to compile with esbuild

This is included in `make build` but must be run manually if only working on the extension.

### Testing Strategy

The project has extensive cross-platform testing:
- Unit tests in each module (`commonTest`, `jvmTest`, `jsTest`)
- Integration tests via `scripts/test.sh` (validates all artifacts)
- Emitter tests verify code generation for each language
- Converter tests validate round-trip conversions

Always run `make test` before submitting changes to ensure all platforms pass.

## Project-Specific Workflows

### Adding a New Language Emitter

1. Create module under `src/compiler/emitters/<language>/`
2. Implement the `Emitter` interface
3. Add language-specific type mappings and code generation
4. Create comprehensive tests in `src/commonTest/`
5. Add language to CLI supported languages list
6. Update `scripts/test.sh` to include the new language
7. Add integration examples if needed

### Adding OpenAPI or Avro Conversion Support

1. For parsers: Add to `src/converter/{openapi,avro}/src/commonMain/.../parse/`
2. For emitters: Add to `src/converter/{openapi,avro}/src/commonMain/.../emit/`
3. Ensure bidirectional conversion maintains schema fidelity
4. Add converter tests validating round-trip conversion

### Modifying the CLI

1. Main CLI code: `src/plugin/cli/src/commonMain/kotlin/community/flock/wirespec/plugin/cli/`
2. Uses Clikt framework for command parsing
3. Supports `compile` and `convert` subcommands
4. After changes, rebuild with `./gradlew :src:plugin:cli:build`
5. Test all three artifacts (native, JVM jar, Node.js) with `make test`

## Release Process

Releases are created via GitHub UI at https://github.com/flock-community/wirespec/releases/new:
1. Create tag following pattern `v*.*.*`
2. Enter release title `Release *.*.*`
3. Click "Publish release"
4. CI will build and publish artifacts to Maven Central, NPM, and Docker Hub