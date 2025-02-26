plugins {
    alias(libs.plugins.gradlebom.plugin)
}

group = "${libs.versions.group.id.get()}"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()
