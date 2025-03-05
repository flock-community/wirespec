plugins {
    alias(libs.plugins.gradlebom.generator)
}

group = libs.versions.group.id.get()
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

bomGenerator {
    includeDependency("$group.compiler", "core-jvm", "$version")
    includeDependency("$group.compiler", "lib-jvm", "$version")
    includeDependency("$group.converter", "avro-jvm", "$version")
    includeDependency("$group.converter", "openapi-jvm", "$version")
    includeDependency("$group.integration", "avro-jvm", "$version")
    includeDependency("$group.integration", "jackson-jvm", "$version")
    includeDependency("$group.integration", "spring-jvm", "$version")
    includeDependency("$group.integration", "wirespec-jvm", "$version")
}
