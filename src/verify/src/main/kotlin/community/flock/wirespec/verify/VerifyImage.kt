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
    },
    RUST {
        override val image by lazy {
            ImageFromDockerfile("wirespec-rust-verify", false)
                .withDockerfileFromBuilder { builder ->
                    builder
                        .from("rust:1.83-slim")
                        .run("cargo init /app && cd /app && cargo add regex serde --features serde/derive && cargo add serde_json && cargo add pollster")
                        .build()
                }
                .get()
        }
    },
    SCALA {
        override val image by lazy {
            ImageFromDockerfile("wirespec-scala-verify", false)
                .withDockerfileFromBuilder { builder ->
                    builder
                        .from("eclipse-temurin:17-jdk")
                        .run("apt-get update -qq && apt-get install -y -qq curl > /dev/null 2>&1")
                        .run(
                            "curl -sSLf https://scala-cli.virtuslab.org/get | sh && " +
                                "ln -s /root/.cache/scalacli/local-repo/bin/scala-cli/scala-cli /usr/local/bin/scala-cli && " +
                                "scala-cli version"
                        )
                        .build()
                }
                .get()
        }
    },
    TYPESCRIPT {
        override val image by lazy {
            ImageFromDockerfile("wirespec-typescript-verify", false)
                .withDockerfileFromBuilder { builder ->
                    builder
                        .from("node:20-slim")
                        // Pinned: typescript@7 (published 2026-07-08) breaks the verify harness;
                        // 6.0.3 is the last version that accepts this harness's tsconfig
                        // (ignoreDeprecations "6.0" is rejected by 5.x)
                        .run("npm install -g typescript@6.0.3 tsx@4.23.0")
                        .build()
                }
                .get()
        }
    };

    abstract val image: String
}
