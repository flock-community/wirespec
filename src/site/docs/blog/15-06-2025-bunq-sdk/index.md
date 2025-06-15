---
title: Bunq SDK
slug: /bunq-sdk
description:  One Specification, Four SDKs
authors: [wilmveel]
tags: [sdk]
image: /img/blog/bunq-hero.png
---

# Bunq SDK: One Specification, Four SDKs

## Introduction

The Bunq SDK project emerged from the Flock innovation days (https://flock.community). During these innovation days, the team took on an exciting challenge: to generate four different SDKs (Java, Kotlin, TypeScript/JavaScript, and Python) from a single OpenAPI specification using Wirespec (https://wirespec.io).

This approach addresses a common problem in API development - maintaining consistent client libraries across multiple programming languages. By using a single source of truth (the OpenAPI specification) and an automated code generation tool, the team was able to ensure that all SDKs provide the same functionality and follow the same patterns, while still feeling native to each language.

## Challenges of the Project

Creating SDKs from an OpenAPI specification isn't always straightforward. The team faced several challenges during the implementation:

### Specification vs. Implementation Mismatches

One of the primary challenges was that the OpenAPI specification wasn't perfectly aligned with the actual API implementation. This is a common issue in API development, where documentation and implementation can drift apart over time.

To address this, the team created a preprocessing step for the OpenAPI specification. Looking at the `OpenApiPreProcessor.kt` file, we can see how they handled these mismatches:

1. **Technical Parameters in the Specification**: The OpenAPI specification included technical parameters that shouldn't be exposed in an SDK. The preprocessor filters out parameters like `Cache-Control`, `User-Agent`, `X-Bunq-Language`, and others that are implementation details rather than part of the API's business logic.

```kotlin
private val filterParams = listOf(
    "Cache-Control",
    "User-Agent",
    "X-Bunq-Language",
    "X-Bunq-Region",
    "X-Bunq-Client-Request-Id",
    "X-Bunq-Geolocation",
    "X-Bunq-Client-Authentication"
)
```

2. **Response Wrapping**: Some API responses were wrapped in implementation-specific structures. The preprocessor adds wrappers for these objects to ensure the generated code correctly handles the actual API responses.

```kotlin
private val wrapResponse = mapOf(
    "DeviceServerCreate" to "Id",
    "MonetaryAccountBankRead" to "MonetaryAccountBank"
)
```

By preprocessing the OpenAPI specification, the team ensured that the generated SDKs would be clean, user-friendly, and accurately reflect the actual API behavior.

## SDK Implementation

The implementation of the SDK follows a similar pattern across all four languages. Let's look at the Kotlin implementation as a reference:

### Configuration

The SDK starts with a `Config` class that serves as the bootstrap for the entire SDK. This class holds essential information like the API key, service name, and paths to key files:

```kotlin
data class Config(
    val serviceName: String,
    val apiKey: String,
    val privateKeyFile: File = File("../private_key.pem"),
    val publicKeyFile: File = File("../public_key.pem"),
    val userAgent: String? = null,
    val cacheControl: String? = null,
    val language: String? = null,
    val region: String? = null,
    val clientRequestId: String? = null,
    val geolocation: String? = null,
)
```

### Signing Logic

Security is a critical aspect of the Bunq API, which requires request signing and response verification. The `Signing` class handles all cryptographic operations:

- Generating RSA key pairs
- Converting keys to PEM format
- Signing request data with SHA256withRSA
- Verifying response signatures

```kotlin
fun signData(data: String): String {
    val privateKey = loadPrivateKey()
    // Ensure the data is encoded in UTF-8 exactly as it will be sent
    val encodedData = data.toByteArray(StandardCharsets.UTF_8)

    // Generate signature using SHA256 and PKCS#1 v1.5 padding
    val signature = Signature.getInstance("SHA256withRSA")
        .apply {
            initSign(privateKey)
            update(encodedData)
        }
        .sign()

    // Encode in Base64
    return Base64.getEncoder().encodeToString(signature)
}
```

### Context and API Handshake

The `Context` class and `initContext` function handle the handshake process with the Bunq API, which involves several steps:

1. Generating an RSA key pair
2. Creating an installation with the public key
3. Creating a device server with the API key and token
4. Creating a session server to get a session token

```kotlin
fun initContext(config: Config): Context {
    val signing = Signing(config)
    val (_, publicKeyPem) = signing.generateRsaKeyPair()
    val installation = createInstallation(config.serviceName, publicKeyPem)
    val deviceServer = createDeviceServer(config.serviceName, config.apiKey, installation.Token.token)
    val serverSession = createSessionServer(config.serviceName, config.apiKey, installation.Token.token)

    return Context(
        apiKey = config.apiKey,
        serviceName = config.serviceName,
        serverPublicKey = installation.ServerPublicKey.server_public_key,
        deviceId = deviceServer.Id.id,
        sessionId = serverSession.Id.id,
        sessionToken = serverSession.Token.token,
        userId = serverSession.UserPerson.id,
        // ... other properties
    )
}
```

### Wirespec Client Implementation

The `Wirespec.kt` file implements the client that interacts with the Bunq API:

- Custom serialization/deserialization logic for Bunq's response format
- HTTP request handling with proper signing
- Response processing and error handling
- Helper functions for creating request handlers

```kotlin
fun send(signing: Signing, req: Wirespec.RawRequest): Wirespec.RawResponse {
    // ... HTTP client setup ...

    // Sign request if it has a body
    if (req.body != null) {
        headers += arrayOf("X-Bunq-Client-Signature", signing.signData(req.body!!))
    }

    // Send HTTP request
    val response = client.send(requestBuilder.build(), java.net.http.HttpResponse.BodyHandlers.ofString())

    // Return Wirespec.RawResponse
    return Wirespec.RawResponse(
        statusCode = response.statusCode(),
        headers = response.headers().map(),
        body = response.body()
    )
}
```

### Dependency-Free and HTTP Client Agnostic Design

One of the most powerful aspects of the Wirespec-generated SDK is its dependency-free and HTTP client agnostic design. Looking at the implementation in `Wirespec.kt`, we can see that:

1. **HTTP Client Isolation**: The SDK uses Java's standard `HttpClient` in the `send` function, but this implementation is completely isolated from the rest of the code:

```kotlin
fun send(signing: Signing, req: Wirespec.RawRequest): Wirespec.RawResponse {
    val client = java.net.http.HttpClient.newBuilder().build()

    // ... request preparation ...

    // Send HTTP request
    val response = client.send(requestBuilder.build(), java.net.http.HttpResponse.BodyHandlers.ofString())

    // Return abstract response
    return Wirespec.RawResponse(
        statusCode = response.statusCode(),
        headers = response.headers().map(),
        body = response.body()
    )
}
```

2. **Abstract Request/Response Model**: The SDK uses `Wirespec.RawRequest` and `Wirespec.RawResponse` as abstract representations of HTTP requests and responses, which are independent of any specific HTTP client implementation.

3. **Clean Interface**: The handler functions provide a clean interface for making API calls without exposing HTTP client details:

```kotlin
fun handler(signing: Signing, context: Context): (Wirespec.Request<*>) -> Wirespec.Response<*> =
    { req -> handle(signing, context, req) }
```

This design offers several significant benefits:

- **No Dependency Conflicts**: Since the SDK doesn't force any specific HTTP client library on the user, it won't conflict with other libraries in the project that might use different HTTP client versions.

- **Freedom of Choice**: Developers can implement their preferred HTTP client (such as OkHttp, Apache HttpClient, or Ktor) by simply replacing the `send` function with their own implementation.

- **Future-Proof**: As HTTP client libraries evolve or new ones emerge, the SDK can be easily updated without breaking changes to its public API.

- **Lightweight**: The SDK doesn't bring unnecessary dependencies into your project, keeping it lean and reducing the risk of version conflicts.

This approach is particularly valuable in large projects where dependency management can become complex, or in environments where specific HTTP client implementations are preferred for performance, security, or compatibility reasons.

## Using the SDK

The SDK provides a clean and intuitive API for interacting with the Bunq API. Looking at the `ApiTest.kt` file, we can see how to use the SDK in practice:

1. **Initialize the SDK**:
   ```kotlin
   val config = Config(
       apiKey = "your_api_key",
       serviceName = "YourServiceName",
       publicKeyFile = File("path/to/public_key.pem"),
       privateKeyFile = File("path/to/private_key.pem"),
   )
   val signing = Signing(config)
   val context = initContext(config)
   val sdk = Sdk(handler(signing, context))
   ```

2. **Make API Calls**:
   ```kotlin
   // Get user information
   val userResponse = sdk.rEAD_User(context.userId)
   val userData = when (userResponse) {
       is READ_User.Response200 -> userResponse.body
       is READ_User.Response400 -> error("Cannot read user")
   }

   // List bank accounts
   val accountsResponse = sdk.list_all_MonetaryAccountBank_for_User(context.userId)
   val accounts = when (accountsResponse) {
       is List_all_MonetaryAccountBank_for_User.Response200 -> accountsResponse.body
       is List_all_MonetaryAccountBank_for_User.Response400 -> error("Could not get bank accounts")
   }
   ```

3. **Process Responses**:
   ```kotlin
   // Access user data
   val userName = userData.UserPerson?.legal_name

   // Access account data
   val accountId = accounts.firstOrNull()?.id
   val balance = accounts.firstOrNull()?.balance?.value
   ```

The SDK handles all the complexities of authentication, request signing, and response parsing, allowing developers to focus on their application logic rather than API integration details.

## Conclusion

The Bunq SDK project demonstrates the power of code generation from a single source of truth. By using Wirespec to generate SDKs for multiple programming languages, the team was able to:

1. Ensure consistency across all SDKs
2. Reduce maintenance overhead
3. Provide a native-feeling API for each language
4. Handle complex authentication and security requirements transparently
5. Create dependency-free and HTTP client agnostic SDKs that won't conflict with project dependencies

This approach is particularly valuable for APIs with complex security requirements like Bunq, where implementing the authentication and signing logic correctly is critical but can be error-prone when done manually.

The preprocessing step shows how even imperfect OpenAPI specifications can be adapted to generate clean, usable SDKs. By addressing specification-implementation mismatches before code generation, the team ensured that the resulting SDKs would be both accurate and user-friendly.

As APIs continue to be a critical part of modern software development, tools like Wirespec that enable consistent, high-quality client libraries across multiple languages will become increasingly valuable. The dependency-free and HTTP client agnostic nature of the generated SDKs is particularly important, as it ensures that the SDKs can be easily integrated into any project without causing dependency conflicts, while allowing developers to use their preferred HTTP client implementation.

The Bunq SDK project serves as an excellent example of how to leverage these tools effectively to create SDKs that are not only consistent and maintainable but also flexible and adaptable to different project requirements.
