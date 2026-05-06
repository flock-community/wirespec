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
import community.flock.wirespec.integration.kotest.kotestWirespecGenerator

/**
 * Per-type convenience factories on top of [kotestWirespecGenerator].
 * `@Generator("email")`, `@Generator("fullname")` etc. on `.ws` fields are
 * routed automatically; `@Seed`-annotated fields use the supplied path.
 */
object TestGenerators {

    private fun gen(seed: Long) = kotestWirespecGenerator(seed = seed)

    fun memberInput(seed: Long = 0L): MemberInput = MemberInputGenerator.generate(gen(seed), emptyList())
    fun member(seed: Long = 0L): Member = MemberGenerator.generate(gen(seed), emptyList())
    fun projectList(seed: Long = 0L): ProjectList = ProjectListGenerator.generate(gen(seed), emptyList())
    fun projectInput(seed: Long = 0L): ProjectInput = ProjectInputGenerator.generate(gen(seed), emptyList())
    fun project(seed: Long = 0L): Project = ProjectGenerator.generate(gen(seed), emptyList())
    fun project(id: String, seed: Long = 0L): Project = ProjectGenerator.generate(gen(seed), listOf(id))
    fun taskInput(seed: Long = 0L): TaskInput = TaskInputGenerator.generate(gen(seed), emptyList())
    fun task(seed: Long = 0L): Task = TaskGenerator.generate(gen(seed), emptyList())
    fun task(id: Long, seed: Long = 0L): Task = TaskGenerator.generate(gen(seed), listOf(id.toString()))
    fun taskList(seed: Long = 0L): TaskList = TaskListGenerator.generate(gen(seed), emptyList())
}
