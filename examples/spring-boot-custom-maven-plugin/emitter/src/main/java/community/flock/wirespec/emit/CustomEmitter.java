package community.flock.wirespec.emit;

import community.flock.wirespec.compiler.core.emit.common.DefinitionModelEmitter;
import community.flock.wirespec.compiler.core.emit.common.Emitted;
import community.flock.wirespec.compiler.core.emit.common.Emitter;
import community.flock.wirespec.compiler.core.parse.Channel;
import community.flock.wirespec.compiler.core.parse.Definition;
import community.flock.wirespec.compiler.core.parse.Endpoint;
import community.flock.wirespec.compiler.core.parse.Enum;
import community.flock.wirespec.compiler.core.parse.Field;
import community.flock.wirespec.compiler.core.parse.Node;
import community.flock.wirespec.compiler.core.parse.Refined;
import community.flock.wirespec.compiler.core.parse.Type;
import community.flock.wirespec.compiler.core.parse.Union;
import community.flock.wirespec.compiler.utils.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class CustomEmitter extends Emitter implements DefinitionModelEmitter {

    public CustomEmitter(@NotNull Logger logger, boolean split) {
        super(logger, split);
    }

    @NotNull
    @Override
    public String emitName(@NotNull Definition definition) {
        return definition.getIdentifier() + "Custom";
    }

    @NotNull
    @Override
    public String notYetImplemented() {
        return "";
    }

    @NotNull
    @Override
    public List<Emitted> emit(@NotNull List<? extends Node> ast) {
        return ast
                .stream()
                .filter(sc -> sc instanceof Type)
                .map(it -> (Type) it)
                .map(type -> new Emitted(emitName(type), emit(type, ast)))
                .collect(Collectors.toList());
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
    public String emit(@NotNull Enum anEnum) {
        return notYetImplemented();
    }

    @NotNull
    @Override
    public String emit(@NotNull Union aUnion) {
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
    public String emit(@NotNull Type type, @NotNull List<? extends Node> ast) {
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
    public String emit(@NotNull Field.Reference reference) {
        return notYetImplemented();
    }
}
