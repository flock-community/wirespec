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
        key to value
    }
    .orEmpty()