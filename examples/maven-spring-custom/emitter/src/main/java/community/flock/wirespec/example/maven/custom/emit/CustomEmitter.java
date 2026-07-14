package community.flock.wirespec.example.maven.custom.emit;

import community.flock.wirespec.compiler.core.emit.EmitShared;
import community.flock.wirespec.compiler.core.emit.PackageName;
import community.flock.wirespec.compiler.core.parse.ast.Definition;
import community.flock.wirespec.compiler.core.parse.ast.Module;
import community.flock.wirespec.compiler.utils.Logger;
import community.flock.wirespec.emitters.java.JavaIrEmitter;
import community.flock.wirespec.ir.core.File;
import community.flock.wirespec.ir.core.Name;
import community.flock.wirespec.ir.core.Package;
import community.flock.wirespec.ir.core.RawElement;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A custom emitter built on the IR pipeline. It extends the standard
 * {@link JavaIrEmitter} and replaces the IR {@link File} produced for every
 * Wirespec definition with a minimal custom class in the {@code hello}
 * package. Any IR element (structs, functions, raw code, ...) can be emitted
 * this way; the generator turns the IR tree into Java source.
 */
public class CustomEmitter extends JavaIrEmitter {

    public CustomEmitter(PackageName packageName) {
        super(packageName, new EmitShared(false));
    }

    @NotNull
    @Override
    public File emit(@NotNull Definition definition, @NotNull Module module, @NotNull Logger logger) {
        var className = definition.getIdentifier().getValue() + "Custom";
        return new File(
                new Name(className),
                List.of(new Package("hello"), new RawElement("public class " + className + " {}")));
    }

    @Nullable
    @Override
    public File emitGenerator(@NotNull Definition definition, @NotNull Module module) {
        return null;
    }
}
