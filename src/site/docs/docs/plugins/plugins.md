---
sidebar_position: 1
---

# Plugins

Wirespec supports various plugins for integration into a variety of ecosystems. These plugins are multiplatform, meaning they are written in the corresponding language of the target platform.

## Operations

All plugins support three core operations: Compile, Convert, and Custom.

### Compile

The `Compile` operation transforms Wirespec source code into emitted output files. It accepts the following inputs:

*   **input:** Path to the input Wirespec file or directory.
*   **output:** Path to the output directory where the generated code will be placed.
*   **languages:** A comma-separated list of target languages for code generation (e.g., `Java`, `Kotlin`, `TypeScript`, `Python`, `Wirespec`, `OpenAPIV2`, `OpenAPIV3`).
*   **package name:** The package name for the generated code.
*   **share:**  A flag to indicate whether shared code should be emitted.
*   **strict:** A flag to enable strict mode during compilation.

[Playground compile](http://playground.wirespec.io/compile)

### Convert

The `Convert` operation facilitates integration with other API specification languages by providing an automated way to convert them to Wirespec. It accepts the following inputs:

*   **input:** Path to the input file in the original specification language.
*   **output:** Path to the output directory where the converted Wirespec file will be placed.
*   **format:** The format of the input file (e.g., `OpenAPIV2`, `OpenAPIV3`, `Avro`).

[Playground convert](http://playground.wirespec.io/covert)

### Custom

The `Custom` operation combines the functionality of both `Compile` and `Convert` and offers additional options for integrating with custom emitters. It accepts the following inputs:

*   **input:** Path to the input file or folder.
*   **output:** Path to the output directory.
*   **format:** Input format (e.g., `OpenAPIV2`, `OpenAPIV3`, `Avro`).
*   **languages:** A comma-separated list of target languages for code generation (e.g., `Java`, `Kotlin`, `TypeScript`, `Python`, `Wirespec`,`OpenAPIV2`, `OpenAPIV3`).
*   **package name:** The package name for the generated code.
*   **share:** A flag to indicate whether shared code should be emitted.
*   **strict:** A flag to enable strict mode during processing.
*   **emitterClass:** The fully qualified name of the custom emitter class.
*   **extension:** The file extension for the output files.
*   **split:** A boolean flag indicating whether to split the output into separate files.