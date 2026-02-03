package community.flock.wirespec.language.generator

import community.flock.wirespec.language.core.Element

interface CodeGenerator {
    fun generate(element: Element): String
}
