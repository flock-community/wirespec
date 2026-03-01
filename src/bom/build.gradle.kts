plugins {
    id("module.publication")
    `java-platform`
}

group = libs.versions.group.id.get()
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

dependencies {
    constraints {
        api("$group.compiler:core-jvm:$version")
        api("$group.compiler:lib-jvm:$version")
        api("$group.converter:avro-jvm:$version")
        api("$group.converter:openapi-jvm:$version")
        api("$group.integration:avro-jvm:$version")
        api("$group.integration:jackson-jvm:$version")
        api("$group.integration:spring-jvm:$version")
        api("$group.integration:wirespec-jvm:$version")
    }
}

publishing {
    publications {
        create<MavenPublication>("bom") {
            from(components["javaPlatform"])
        }
    }
}
