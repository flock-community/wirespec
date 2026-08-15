package community.flock.wirespec.examples.spring.testutil

import community.flock.wirespec.examples.spring.generated.generator.MemberGenerator
import community.flock.wirespec.examples.spring.generated.generator.MemberInputGenerator
import community.flock.wirespec.examples.spring.generated.generator.ProjectGenerator
import community.flock.wirespec.examples.spring.generated.generator.ProjectInputGenerator
import community.flock.wirespec.examples.spring.generated.generator.ProjectListGenerator
import community.flock.wirespec.examples.spring.generated.generator.TaskGenerator
import community.flock.wirespec.examples.spring.generated.generator.TaskInputGenerator
import community.flock.wirespec.examples.spring.generated.generator.TaskListGenerator
import community.flock.wirespec.examples.spring.generated.model.Member
import community.flock.wirespec.examples.spring.generated.model.MemberInput
import community.flock.wirespec.examples.spring.generated.model.Project
import community.flock.wirespec.examples.spring.generated.model.ProjectInput
import community.flock.wirespec.examples.spring.generated.model.ProjectList
import community.flock.wirespec.examples.spring.generated.model.Task
import community.flock.wirespec.examples.spring.generated.model.TaskInput
import community.flock.wirespec.examples.spring.generated.model.TaskList
import community.flock.wirespec.integration.kotest.kotestWirespecKotlinGenerator

/**
 * Per-type convenience factories on top of [kotestWirespecKotlinGenerator].
 * `@Generator("email")`, `@Generator("fullname")` etc. on `.ws` fields are
 * routed automatically; `@Seed`-annotated fields use the supplied path.
 */
object TestGenerators {

    private fun generator(seed: Long) = kotestWirespecKotlinGenerator(seed = seed)

    fun memberInput(seed: Long = 0L): MemberInput = MemberInputGenerator.generate(generator(seed), emptyList())
    fun member(id: String, seed: Long = 0L): Member = MemberGenerator.generate(generator(seed), listOf(id.toString()))
    fun projectList(seed: Long = 0L): ProjectList = ProjectListGenerator.generate(generator(seed), emptyList())
    fun projectInput(seed: Long = 0L): ProjectInput = ProjectInputGenerator.generate(generator(seed), emptyList())
    fun project(seed: Long = 0L): Project = ProjectGenerator.generate(generator(seed), emptyList())
    fun project(id: String, seed: Long = 0L): Project = ProjectGenerator.generate(generator(seed), listOf(id))
    fun taskInput(seed: Long = 0L): TaskInput = TaskInputGenerator.generate(generator(seed), emptyList())
    fun task(seed: Long = 0L): Task = TaskGenerator.generate(generator(seed), emptyList())
    fun task(id: Long, seed: Long = 0L): Task = TaskGenerator.generate(generator(seed), listOf(id.toString()))
    fun taskList(seed: Long = 0L): TaskList = TaskListGenerator.generate(generator(seed), emptyList())
}
