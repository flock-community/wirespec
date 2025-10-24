package community.flock.wirespec.example.maven.custom.emit;


import community.flock.wirespec.compiler.core.emit.LanguageEmitter;
import community.flock.wirespec.compiler.core.emit.FileExtension;
import community.flock.wirespec.compiler.core.emit.Shared;
import community.flock.wirespec.compiler.core.parse.ast.Channel;
import community.flock.wirespec.compiler.core.parse.ast.Endpoint;
import community.flock.wirespec.compiler.core.parse.ast.Field;
import community.flock.wirespec.compiler.core.parse.ast.Identifier;
import community.flock.wirespec.compiler.core.parse.ast.Reference;
import community.flock.wirespec.compiler.core.parse.ast.Module;
import community.flock.wirespec.compiler.core.parse.ast.Refined;
import community.flock.wirespec.compiler.core.parse.ast.Type;
import community.flock.wirespec.compiler.core.parse.ast.Enum;
import community.flock.wirespec.compiler.core.parse.ast.Union;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomEmitter extends LanguageEmitter {

    @NotNull
    @Override
    public String getSingleLineComment() {
        return "//";
    }

    @NotNull
    @Override
    public FileExtension getExtension() {
        return FileExtension.Java;
    }

    @Nullable
    @Override
    public Shared getShared() {
        return null;
    }

    @NotNull
    @Override
    public String notYetImplemented() {
        return "";
    }

    @NotNull
    @Override
    public String emit(@NotNull Endpoint endpoint) {
        return notYetImplemented();
    }

    @NotNull
    @Override
    public String emit(@NotNull Channel channel) {
        return notYetImplemented();
    }

    @NotNull
    @Override
    public String emit(@NotNull Enum anEnum, @NotNull Module module) {
        return notYetImplemented();
    }

    @NotNull
    @Override
    public String emit(@NotNull Union union) {
        return notYetImplemented();
    }

    @NotNull
    @Override
    public String emit(@NotNull Refined refined) {
        return notYetImplemented();
    }

    @NotNull
    @Override
    public String emit(@NotNull Type type, @NotNull Module module) {
        return "package hello;\n\npublic class " + emit(type.getIdentifier()) + " {}";
    }

    @NotNull
    @Override
    public String emit(@NotNull Type.Shape shape) {
        return notYetImplemented();
    }

    @NotNull
    @Override
    public String emit(@NotNull Field field) {
        return notYetImplemented();
    }

    @NotNull
    @Override
    public String emit(@NotNull Reference reference) {
        return notYetImplemented();
    }

    @NotNull
    @Override
    public String emit(@NotNull Identifier identifier) {
        return identifier.getValue() + "Custom";
    }

    @NotNull
    @Override
    public String emitValidator(@NotNull Refined refined)  {
        return notYetImplemented();
    }

    @NotNull
    @Override
    public String emit(@NotNull Reference.Primitive.Type.Constraint constraint)  {
        return notYetImplemented();
    }
}
