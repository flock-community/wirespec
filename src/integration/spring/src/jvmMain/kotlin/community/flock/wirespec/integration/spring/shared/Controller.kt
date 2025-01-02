package community.flock.wirespec.integration.spring.shared

import jakarta.servlet.http.HttpServletRequest

/**
 * Depending on how the controller is mapped:
 *
 * @RequestMapping("/pet/{id}")
 * // or
 * @GetMapping("/pet/{id}")
 *
 * The path ends up in either the pathInfo or servletPath
 */
fun HttpServletRequest.extractPath() = (if (pathInfo != null) pathInfo else servletPath)
    .split("/")
    .filter { it.isNotEmpty() }

fun HttpServletRequest.extractQueries() = queryString
    ?.split("&")
    ?.associate {
        val (key, value) = it.split("=")
        // TODO here we need to do something with different strategies for arrays and objects
        key to listOf(value)
    }
    .orEmpty()


data class QueryParamSerializationConfig(val arrayFormat: ArrayFormat)

sealed interface ArrayFormat {
    data object Exploded : ArrayFormat
    data class Delimited(val delimiter: String) : ArrayFormat
}
