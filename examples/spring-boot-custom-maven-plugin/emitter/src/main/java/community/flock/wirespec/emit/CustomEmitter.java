package community.flock.wirespec.emit;

import community.flock.wirespec.compiler.core.emit.common.Emitter;
import community.flock.wirespec.compiler.core.parse.Node;
import community.flock.wirespec.compiler.core.parse.Refined;
import community.flock.wirespec.compiler.core.parse.Type;
import community.flock.wirespec.compiler.core.parse.Endpoint;
import community.flock.wirespec.compiler.utils.Logger;
import kotlin.Pair;

import java.util.List;
import java.util.stream.Collectors;

public class CustomEmitter extends Emitter {

    public CustomEmitter(Logger logger, boolean split) {
        super(logger, split);
    }

    @Override
    public List<Pair<String, String>> emit(List<? extends Node> ast) {
        return super.emit(ast)
                .stream()
                .map(a -> new Pair<String,String>(a.component1(), "package hello;\n\n" + a.component2()))
                .collect(Collectors.toList());
    }

    @Override
    public String emit(Refined refined) {
        return null;
    }


    @Override
    public String emit(Refined.Validator validator) {
        return null;
    }


    @Override
    public String emit(Type type) {
        return "public class " + type.getName() + " {}";
    }


    @Override
    public String emit(Type.Shape shape) {
        return null;
    }


    @Override
    public String emit(Type.Shape.Field field) {
        return null;
    }


    @Override
    public String emit(Type.Shape.Field.Identifier identifier) {
        return null;
    }


    @Override
    public String emit(Type.Shape.Field.Reference reference) {
        return null;
    }

    @Override
    public String emit(Endpoint endpoint) {
        return null;
    }
}