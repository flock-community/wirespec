package community.flock.wirespec.integration.spring.java.emit;

import community.flock.wirespec.compiler.core.emit.EmitShared;
import community.flock.wirespec.compiler.core.emit.EndpointEmitterExtensionsKt;
import community.flock.wirespec.compiler.core.emit.PackageName;
import community.flock.wirespec.compiler.core.emit.Spacer;
import community.flock.wirespec.compiler.core.parse.Endpoint;
import community.flock.wirespec.emitters.java.JavaEmitter;

import java.util.stream.Collectors;

public class SpringJavaEmitter extends JavaEmitter {
    public SpringJavaEmitter(PackageName packageName) {
        super(packageName, new EmitShared(false));
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
            case HEAD -> "@org.springframework.web.bind.annotation.RequestMapping(value=\"" + path + "\", method = org.springframework.web.bind.annotation.RequestMethod.HEAD)";
            case PATCH -> "@org.springframework.web.bind.annotation.RequestMapping(value=\"" + path + "\", method = org.springframework.web.bind.annotation.RequestMethod.PATCH)";
            case TRACE -> "@org.springframework.web.bind.annotation.RequestMapping(value=\"" + path + "\", method = org.springframework.web.bind.annotation.RequestMethod.TRACE)";
        };
        
        return annotation + "\n" +
                Spacer.INSTANCE.invoke(2) + super.emitHandleFunction(endpoint) + "\n";
    }
}
