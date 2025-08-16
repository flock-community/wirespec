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

    format("misc") {
        target("**/*.md")
        targetExclude(*exclude)
        endWithNewline()
    }

    format("wirespec") {
        target("**/*.ws")
        targetExclude(*exclude)
        endWithNewline()
    }

    kotlin {
        target("**/*.kt", "**/*.kts")
        targetExclude(*exclude)
        ktlint().editorConfigOverride(
            mapOf("ktlint_code_style" to "intellij_idea"),
        )
        suppressLintsFor {
            step = "ktlint"
            shortCode = "standard:enum-entry-name-case"
        }
    }
}
