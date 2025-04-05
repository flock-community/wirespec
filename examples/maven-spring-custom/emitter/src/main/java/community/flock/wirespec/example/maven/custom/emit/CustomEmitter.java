package community.flock.wirespec.example.maven.custom.emit;

import arrow.core.NonEmptyList;
import community.flock.wirespec.compiler.core.emit.common.Emitted;
import community.flock.wirespec.compiler.core.emit.common.Emitter;
import community.flock.wirespec.compiler.core.emit.common.FileExtension;
import community.flock.wirespec.compiler.core.emit.shared.Shared;
import community.flock.wirespec.compiler.core.parse.Channel;
import community.flock.wirespec.compiler.core.parse.Definition;
import community.flock.wirespec.compiler.core.parse.Endpoint;
import community.flock.wirespec.compiler.core.parse.Enum;
import community.flock.wirespec.compiler.core.parse.Field;
import community.flock.wirespec.compiler.core.parse.Identifier;
import community.flock.wirespec.compiler.core.parse.Node;
import community.flock.wirespec.compiler.core.parse.Reference;
import community.flock.wirespec.compiler.core.parse.Refined;
import community.flock.wirespec.compiler.core.parse.Type;
import community.flock.wirespec.compiler.core.parse.Union;
import community.flock.wirespec.compiler.core.parse.Module;
import community.flock.wirespec.compiler.core.parse.AST;
import community.flock.wirespec.compiler.utils.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomEmitter extends Emitter {

    public CustomEmitter() {
        super();
    }

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
    public String emitName(@NotNull Definition definition) {
        return emit(definition.getIdentifier()) + "Custom";
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
    public String emit(@NotNull Refined.Validator validator) {
        return notYetImplemented();
    }

    @NotNull
    @Override
    public String emit(@NotNull Type type, @NotNull Module module) {
        return "package hello;\n\npublic class " + emitName(type) + " {}";
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
        return identifier.getValue();
    }
}
