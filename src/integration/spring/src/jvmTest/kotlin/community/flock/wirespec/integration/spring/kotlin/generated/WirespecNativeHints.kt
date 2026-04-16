package community.flock.wirespec.integration.spring.kotlin.generated

import community.flock.wirespec.integration.spring.kotlin.generated.model.TodoId
import community.flock.wirespec.integration.spring.kotlin.generated.model.RequestBodyParrot
import community.flock.wirespec.integration.spring.kotlin.generated.model.TodoDto
import community.flock.wirespec.integration.spring.kotlin.generated.model.TodoDtoPatch
import community.flock.wirespec.integration.spring.kotlin.generated.model.Error
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.RequestParrot
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetTodos
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.PatchTodos
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.DownloadReport

import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportRuntimeHints

@Configuration
@ImportRuntimeHints(WirespecNativeHints.GeneratedHints::class)
open class WirespecNativeHints {

  class GeneratedHints : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
      val allMembers = MemberCategory.entries.toTypedArray()
      registerWithInnerClasses(hints, TodoId::class.java, allMembers)
      registerWithInnerClasses(hints, RequestBodyParrot::class.java, allMembers)
      registerWithInnerClasses(hints, TodoDto::class.java, allMembers)
      registerWithInnerClasses(hints, TodoDtoPatch::class.java, allMembers)
      registerWithInnerClasses(hints, Error::class.java, allMembers)
      registerWithInnerClasses(hints, RequestParrot::class.java, allMembers)
      registerWithInnerClasses(hints, GetTodos::class.java, allMembers)
      registerWithInnerClasses(hints, PatchTodos::class.java, allMembers)
      registerWithInnerClasses(hints, DownloadReport::class.java, allMembers)
    }

    private fun registerWithInnerClasses(hints: RuntimeHints, clazz: Class<*>, categories: Array<MemberCategory>) {
      hints.reflection().registerType(clazz, *categories)
      for (inner in clazz.declaredClasses) {
        registerWithInnerClasses(hints, inner, categories)
      }
    }
  }
}
