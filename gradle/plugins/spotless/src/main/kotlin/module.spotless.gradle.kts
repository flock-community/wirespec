plugins {
    id("com.diffplug.spotless")
}

spotless {
    val exclude = listOf(
        "**/*Emitter.kt",
        "**/generated/**",
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
        targetExclude(*exclude, "**/*Emitter.kt")
        ktlint().editorConfigOverride(
            mapOf("ktlint_code_style" to "intellij_idea"),
        )
        suppressLintsFor {
            step = "ktlint"
            shortCode = "standard:enum-entry-name-case"
        }
    }
}
