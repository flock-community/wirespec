package community.flock.wirespec.language.generator

import community.flock.wirespec.language.core.Element

fun Element.generateJava() = JavaGenerator.generate(this)

fun Element.generatePython() = PythonGenerator.generate(this)

fun Element.generateTypeScript() = TypeScriptGenerator.generate(this)

fun Element.generateKotlin() = KotlinGenerator.generate(this)
