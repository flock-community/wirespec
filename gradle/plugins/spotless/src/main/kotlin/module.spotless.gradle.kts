plugins {
    id("com.diffplug.spotless")
}

spotless {
    val exclude = listOf(
        "**/emitters/**/*Emitter.kt",
        "**/build/**",
        "**/generated/**",
        "**/generated-sources/**",
        "**/resources/**",
    ).toTypedArray()

    // Excludes go on the tree itself so the walk never enters them: a string target with
    // targetExclude walks build/ too and fails when a task (e.g. KSP) rewrites it mid-walk.
    fun sources(vararg include: String) = fileTree(projectDir) {
        include(*include)
        exclude(*exclude)
    }

    format("misc") {
        target(sources("**/*.md"))
        endWithNewline()
    }

    format("wirespec") {
        target(sources("**/*.ws"))
        endWithNewline()
    }

    kotlin {
        target(sources("**/*.kt", "**/*.kts"))
        ktlint().editorConfigOverride(
            mapOf("ktlint_code_style" to "intellij_idea"),
        )
        suppressLintsFor {
            step = "ktlint"
            shortCode = "standard:enum-entry-name-case"
        }
    }
}
