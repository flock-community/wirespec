package community.flock.wirespec.emit;

import community.flock.wirespec.compiler.core.emit.common.Emitted;
import community.flock.wirespec.compiler.core.emit.common.Emitter;
import community.flock.wirespec.compiler.core.parse.Endpoint;
import community.flock.wirespec.compiler.core.parse.Enum;
import community.flock.wirespec.compiler.core.parse.Node;
import community.flock.wirespec.compiler.core.parse.Refined;
import community.flock.wirespec.compiler.core.parse.Type;
import community.flock.wirespec.compiler.utils.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class CustomEmitter extends Emitter {

    public CustomEmitter(@NotNull Logger logger, boolean split) {
        super(logger, split);
    }


    @NotNull
    @Override
    public List<Emitted> emit(@NotNull List<? extends Node> ast) {
        return ast
                .stream()
                .filter(sc -> sc instanceof Type)
                .map(it -> (Type) it)
                .map(sc -> new Emitted(sc.getName(), emit(sc)))
                .collect(Collectors.toList());
    }

    @Nullable
    @Override
    public String getShared() {
        return null;
    }

    @NotNull
    @Override
    public String emit(@NotNull Endpoint endpoint) {
        return "";
    }

    @NotNull
    @Override
    public String emit(@NotNull Enum anEnum) {
        return "";
    }

    @NotNull
    @Override
    public String emit(@NotNull Refined refined) {
        return "";
    }

    @NotNull
    @Override
    public String emit(@NotNull Refined.Validator validator) {
        return "";
    }

    @NotNull
    @Override
    public String emit(@NotNull Type type) {
        return "package hello;\n\npublic class " + type.getName() + " {}";
    }

    @NotNull
    @Override
    public String emit(@NotNull Type.Shape shape) {
        return "";
    }

    @NotNull
    @Override
    public String emit(@NotNull Type.Shape.Field field) {
        return "";
    }

    @NotNull
    @Override
    public String emit(@NotNull Type.Shape.Field.Identifier identifier) {
        return "";
    }

    @NotNull
    @Override
    public String emit(@NotNull Type.Shape.Field.Reference reference) {
        return "";
    }
}
