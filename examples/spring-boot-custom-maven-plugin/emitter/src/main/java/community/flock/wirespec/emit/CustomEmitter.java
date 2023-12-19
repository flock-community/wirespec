package community.flock.wirespec.emit;

import community.flock.wirespec.compiler.core.emit.common.Emitter;
import community.flock.wirespec.compiler.core.parse.nodes.Node;
import community.flock.wirespec.compiler.core.parse.nodes.Type;
import community.flock.wirespec.compiler.utils.Logger;
import kotlin.Pair;
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
    public List<Pair<String, String>> emit(@NotNull List<? extends Node> ast) {
        return ast
                .stream()
                .filter(sc -> sc instanceof Type)
                .map(sc -> emit((Type) sc))
                .collect(Collectors.toList());
    }

    public Pair<String, String> emit(Type type) {
        return new Pair<>(type.getName(), "package hello;\n\npublic class " + type.getName() + " {}");
    }

    @Nullable
    @Override
    public String getShared() {
        return "";
    }
}
