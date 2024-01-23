package community.flock.wirespec.integration.jackson.kotlin.generated

import community.flock.wirespec.Wirespec

data class TodoId(override val value: String) : Wirespec.Refined

fun TodoId.validate() =
    Regex("^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$").matches(value)

data class Todo(
    val id: TodoId,
    val name: String,
    val done: Boolean,
    val category: TodoCategory
)

data class TodoInput(
    val name: String,
    val done: Boolean
)

data class Error(
    val code: Long,
    val description: String
)

enum class TodoCategory(public val label: String) : Wirespec.Enum {
    _WORK("WORK"),
    _LIFE("LIFE");

    override fun toString(): String {
        return label
    }
}
