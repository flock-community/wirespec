# gradle-spring-boot

A Kotlin Spring Boot project-management API backend that uses **`SpringKotlinIrEmitter`** (the IR-based Wirespec emitter) and Gradle.

## What this demonstrates

* Wirespec types/endpoints in `src/main/wirespec/projects.ws` are compiled to Kotlin via the `SpringKotlinIrEmitter`, which injects `@GetMapping` / `@PostMapping` / etc. on the generated `Handler` interfaces.
* The generated `Handler` methods are `suspend fun` — Spring MVC executes them on Kotlin coroutines (non-blocking / reactive in the Kotlin sense).
* Three controllers (`ProjectController`, `TaskController`, `MemberController`) implement the generated `Handler` interfaces directly. No additional routing code.
* A small IR-compatible Spring glue layer in `config/` reaches the generated `Handler.Companion` (which implements `Wirespec.Server`) to translate raw HTTP <-> typed requests/responses.
* In-memory repositories use `kotlinx.coroutines` `Mutex` for thread-safe async access.

## Layout

```
src/
├── main/
│   ├── kotlin/.../examples/spring/
│   │   ├── ProjectManagementApplication.kt
│   │   ├── config/
│   │   │   ├── WirespecConfig.kt              # Beans + WebMvcConfigurer
│   │   │   ├── WirespecRequestArgumentResolver.kt
│   │   │   └── WirespecResponseAdvice.kt
│   │   ├── controller/                        # Implements generated Handler interfaces
│   │   ├── service/
│   │   └── repository/
│   ├── resources/application.yml
│   └── wirespec/projects.ws
└── test/
    └── kotlin/.../examples/spring/
        ├── controller/                        # @SpringBootTest + MockMvc
        └── service/                           # Pure coroutine tests
```

## Run

```sh
./gradlew bootRun     # starts the API on http://localhost:8080
./gradlew test        # runs unit + integration tests
./gradlew build       # full build (compile + test + jar)
```

## Why a custom argument-resolver and advice?

The IR emitter generates `fromRawRequest` / `toRawResponse` on each endpoint object, while the published `community.flock.wirespec.integration:spring-jvm` resolver/advice still reflect on the older `fromRequest` / `toResponse` names. The example bypasses that mismatch by using the typed `Wirespec.Server` companion-object API (`server(serialization).from(...)` / `.to(...)`) directly.
