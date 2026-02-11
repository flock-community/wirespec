package community.flock.wirespec.verify

import org.testcontainers.images.builder.ImageFromDockerfile

enum class VerifyImage {
    KOTLIN_1 {
        override val image by lazy {
            val version = "1.9.24"
            ImageFromDockerfile("wirespec-kotlin-verify", false)
                .withDockerfileFromBuilder { builder ->
                    builder
                        .from("eclipse-temurin:17-jdk")
                        .run("apt-get update -qq && apt-get install -y -qq wget unzip > /dev/null 2>&1")
                        .run(
                            "wget -q https://github.com/JetBrains/kotlin/releases/download/v$version/kotlin-compiler-$version.zip -O /tmp/kotlin.zip && " +
                                    "unzip -q /tmp/kotlin.zip -d /opt && " +
                                    "rm /tmp/kotlin.zip"
                        )
                        .build()
                }
                .get()
        }
    },
    KOTLIN_2 {
        override val image by lazy {
            val version = "2.0.21"
            ImageFromDockerfile("wirespec-kotlin-verify", false)
                .withDockerfileFromBuilder { builder ->
                    builder
                        .from("eclipse-temurin:17-jdk")
                        .run("apt-get update -qq && apt-get install -y -qq wget unzip > /dev/null 2>&1")
                        .run(
                            "wget -q https://github.com/JetBrains/kotlin/releases/download/v$version/kotlin-compiler-$version.zip -O /tmp/kotlin.zip && " +
                                "unzip -q /tmp/kotlin.zip -d /opt && " +
                                "rm /tmp/kotlin.zip"
                        )
                        .build()
                }
                .get()
        }
    },
    PYTHON {
        override val image by lazy {
            ImageFromDockerfile("wirespec-python-verify", false)
                .withDockerfileFromBuilder { builder ->
                    builder
                        .from("python:3.12-slim")
                        .run("pip install mypy")
                        .build()
                }
                .get()
        }
    };

    abstract val image: String
}
