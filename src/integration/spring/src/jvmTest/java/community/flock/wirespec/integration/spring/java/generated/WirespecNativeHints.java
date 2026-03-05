package community.flock.wirespec.integration.spring.java.generated;

import community.flock.wirespec.integration.spring.java.generated.model.TodoId;
import community.flock.wirespec.integration.spring.java.generated.model.RequestBodyParrot;
import community.flock.wirespec.integration.spring.java.generated.model.TodoDto;
import community.flock.wirespec.integration.spring.java.generated.model.TodoDtoPatch;
import community.flock.wirespec.integration.spring.java.generated.model.Error;
import community.flock.wirespec.integration.spring.java.generated.endpoint.RequestParrot;
import community.flock.wirespec.integration.spring.java.generated.endpoint.GetTodos;
import community.flock.wirespec.integration.spring.java.generated.endpoint.PatchTodos;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints(WirespecNativeHints.GeneratedHints.class)
public class WirespecNativeHints {

  static class GeneratedHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      var allMembers = MemberCategory.values();
      registerWithInnerClasses(hints, TodoId.class, allMembers);
      registerWithInnerClasses(hints, RequestBodyParrot.class, allMembers);
      registerWithInnerClasses(hints, TodoDto.class, allMembers);
      registerWithInnerClasses(hints, TodoDtoPatch.class, allMembers);
      registerWithInnerClasses(hints, Error.class, allMembers);
      registerWithInnerClasses(hints, RequestParrot.class, allMembers);
      registerWithInnerClasses(hints, GetTodos.class, allMembers);
      registerWithInnerClasses(hints, PatchTodos.class, allMembers);
    }

    private static void registerWithInnerClasses(RuntimeHints hints, Class<?> clazz, MemberCategory[] categories) {
      hints.reflection().registerType(clazz, categories);
      for (Class<?> inner : clazz.getDeclaredClasses()) {
        registerWithInnerClasses(hints, inner, categories);
      }
    }
  }
}
