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
    ?.let { query ->
        query.split("&").flatMap { param ->
            val (key, value) = param.split("=", limit = 2)
            value.split(",").map { key to it }
        }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )
    }
    .orEmpty()