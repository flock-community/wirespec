package community.flock.wirespec.integration.spring.java.emit;

import community.flock.wirespec.compiler.core.emit.Emitted;
import community.flock.wirespec.compiler.core.emit.EndpointEmitterExtensionsKt;
import community.flock.wirespec.compiler.core.emit.PackageName;
import community.flock.wirespec.compiler.core.emit.Spacer;
import community.flock.wirespec.compiler.core.parse.ast.Channel;
import community.flock.wirespec.compiler.core.parse.ast.Definition;
import community.flock.wirespec.compiler.core.parse.ast.Endpoint;
import community.flock.wirespec.compiler.core.parse.ast.Model;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SpringJavaEmitter extends SpringJavaEmitterHelper {
    public SpringJavaEmitter(PackageName packageName) {
        super(packageName);
    }

    @NotNull
    @Override
    public List<Emitted> injectFiles(@NotNull List<? extends Definition> definitions) {
        var modelNames = new ArrayList<String>();
        var endpointNames = new ArrayList<String>();

        for (var definition : definitions) {
            var name = definition.getIdentifier().getValue();
            if (definition instanceof Model) {
                modelNames.add(name);
            } else if (definition instanceof Endpoint || definition instanceof Channel) {
                endpointNames.add(name);
            }
        }

        if (modelNames.isEmpty() && endpointNames.isEmpty()) {
            return Collections.emptyList();
        }

        var file = getPackageName().toDir() + "WirespecNativeHints." + getExtension().getValue();
        return List.of(new Emitted(file, emitNativeHints(modelNames, endpointNames)));
    }

    @Override
    public String emitHandleFunction(Endpoint endpoint) {
        String path = "/" + endpoint.getPath().stream()
                .map(EndpointEmitterExtensionsKt::emit)
                .collect(Collectors.joining("/"));

        String annotation = switch (endpoint.getMethod()) {
            case GET -> "@org.springframework.web.bind.annotation.GetMapping(\"" + path + "\")";
            case POST -> "@org.springframework.web.bind.annotation.PostMapping(\"" + path + "\")";
            case PUT -> "@org.springframework.web.bind.annotation.PutMapping(\"" + path + "\")";
            case DELETE -> "@org.springframework.web.bind.annotation.DeleteMapping(\"" + path + "\")";
            case OPTIONS -> "@org.springframework.web.bind.annotation.RequestMapping(value=\"" + path + "\", method = org.springframework.web.bind.annotation.RequestMethod.OPTIONS)";
            case PATCH -> "@org.springframework.web.bind.annotation.RequestMapping(value=\"" + path + "\", method = org.springframework.web.bind.annotation.RequestMethod.PATCH)";
            case HEAD -> "@org.springframework.web.bind.annotation.RequestMapping(value=\"" + path + "\", method = org.springframework.web.bind.annotation.RequestMethod.HEAD)";
            case TRACE -> "@org.springframework.web.bind.annotation.RequestMapping(value=\"" + path + "\", method = org.springframework.web.bind.annotation.RequestMethod.TRACE)";
        };

        return annotation + "\n" +
                Spacer.INSTANCE.invoke(2) + super.emitHandleFunction(endpoint) + "\n";
    }

    private String emitNativeHints(List<String> modelNames, List<String> endpointNames) {
        var pkg = getPackageName().getValue();

        var modelImports = modelNames.stream()
                .map(name -> "import " + pkg + ".model." + name + ";")
                .collect(Collectors.joining("\n"));
        var endpointImports = endpointNames.stream()
                .map(name -> "import " + pkg + ".endpoint." + name + ";")
                .collect(Collectors.joining("\n"));
        var modelRegistrations = modelNames.stream()
                .map(name -> "      registerWithInnerClasses(hints, " + name + ".class, allMembers);")
                .collect(Collectors.joining("\n"));
        var endpointRegistrations = endpointNames.stream()
                .map(name -> "      registerWithInnerClasses(hints, " + name + ".class, allMembers);")
                .collect(Collectors.joining("\n"));

        return """
                package %s;

                %s
                %s

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
                %s
                %s
                    }

                    private static void registerWithInnerClasses(RuntimeHints hints, Class<?> clazz, MemberCategory[] categories) {
                      hints.reflection().registerType(clazz, categories);
                      for (Class<?> inner : clazz.getDeclaredClasses()) {
                        registerWithInnerClasses(hints, inner, categories);
                      }
                    }
                  }
                }
                """.formatted(pkg, modelImports, endpointImports, modelRegistrations, endpointRegistrations);
    }
}
