package community.flock.wirespec.openapi.common

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.emitters.kotlin.KotlinEmitter

internal fun compile(source: String) = object : CompilationContext, NoLogger {
    override val emitters = nonEmptySetOf(KotlinEmitter())
}.compile(nonEmptyListOf(ModuleContent(FileUri("SomeTestFileUri"), source)))
