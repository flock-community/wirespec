package community.flock.wirespec.plugin.maven

import java.util.Locale

fun String.capitalize() = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
}
