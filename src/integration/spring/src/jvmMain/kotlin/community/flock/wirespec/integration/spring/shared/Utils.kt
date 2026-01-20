package community.flock.wirespec.integration.spring.shared

import jakarta.servlet.http.HttpServletRequest
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Depending on how the controller is mapped:
 *
 * @RequestMapping("/pet/{id}")
 * // or
 * @GetMapping("/pet/{id}")
 *
 * The path ends up in either the pathInfo or servletPath
 */
fun HttpServletRequest.extractPath() = (pathInfo ?: servletPath)
    .split("/")
    .filter { it.isNotEmpty() }

fun HttpServletRequest.extractQueries() = extractQueries(queryString)

fun extractQueries(queryString: String?): Map<String, List<String>> = queryString
    ?.let { query ->
        query.split("&").flatMap { param ->
            val (key, value) = param.split("=", limit = 2)
            val decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8)
            value.split(",").map {
                decodedKey to URLDecoder.decode(it, StandardCharsets.UTF_8)
            }
        }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second },
            )
    }
    .orEmpty()

fun Map<String, List<String>>.filterNotEmpty(): Map<String, List<String>> = filter { it.value.isNotEmpty() }
