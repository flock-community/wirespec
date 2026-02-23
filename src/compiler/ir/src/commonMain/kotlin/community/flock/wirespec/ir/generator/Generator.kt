package community.flock.wirespec.ir.generator

import community.flock.wirespec.ir.core.Element

interface Generator {
    fun generate(element: Element): String
}

fun Element.generateJava() = JavaGenerator.generate(this)

fun Element.generatePython() = PythonGenerator.generate(this)

fun Element.generateTypeScript() = TypeScriptGenerator.generate(this)

fun Element.generateKotlin() = KotlinGenerator.generate(this)

fun Element.generateRust() = RustGenerator.generate(this)
