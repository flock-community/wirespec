package community.flock.wirespec.compiler.common

import kotlin.test.assertTrue

inline fun <reified R : Any> Any.assert(): R = assertTrue { this is R }.let { this as R }
