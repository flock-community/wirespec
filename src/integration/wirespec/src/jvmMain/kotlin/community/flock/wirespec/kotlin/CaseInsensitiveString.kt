package community.flock.wirespec.kotlin

internal fun String.caseInsensitive(): CaseInsensitiveString = CaseInsensitiveString(this)

internal class CaseInsensitiveString(val content: String) {
    private val hash: Int

    init {
        var temp = 0
        for (element in content) {
            temp = temp * 31 + element.lowercaseChar().code
        }

        hash = temp
    }

    override fun equals(other: Any?): Boolean = (other as? CaseInsensitiveString)?.content?.equals(content, ignoreCase = true) == true

    override fun hashCode(): Int = hash

    override fun toString(): String = content
}
