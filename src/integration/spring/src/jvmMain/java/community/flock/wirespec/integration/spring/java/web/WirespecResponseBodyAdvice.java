package community.flock.wirespec.integration.spring.java.web;

import community.flock.wirespec.integration.spring.shared.RawJsonBody;
import community.flock.wirespec.java.Wirespec;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class WirespecResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final Wirespec.Serialization wirespecSerialization;

    public WirespecResponseBodyAdvice(Wirespec.Serialization wirespecSerialization) {
        this.wirespecSerialization = wirespecSerialization;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return Wirespec.Response.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        try {
            Class<?> declaringClass = returnType.getParameterType().getDeclaringClass();
            Class<?> handlerClass = Arrays.stream(declaringClass.getDeclaredClasses())
                    .filter(c -> c.getSimpleName().equals("Handler"))
                    .findFirst().orElse(null);

            Class<?> handlersClass = null;
            if (handlerClass != null) {
                handlersClass = Arrays.stream(handlerClass.getDeclaredClasses())
                        .filter(c -> c.getSimpleName().equals("Handlers"))
                        .findFirst().orElse(null);
            }
            
            if (handlersClass == null) {
                throw new IllegalStateException("Handlers not found");
            }

            Wirespec.Server<Wirespec.Request<?>, Wirespec.Response<?>> instance = 
                    (Wirespec.Server<Wirespec.Request<?>, Wirespec.Response<?>>) handlersClass.getDeclaredConstructor().newInstance();
            
            Wirespec.ServerEdge<Wirespec.Request<?>, Wirespec.Response<?>> server = instance.getServer(wirespecSerialization);

            if (body instanceof Wirespec.Response<?> wirespecResponse) {
                Wirespec.RawResponse rawResponse = server.to(wirespecResponse);
                
                response.setStatusCode(HttpStatusCode.valueOf(rawResponse.statusCode()));
                for (Map.Entry<String, List<String>> entry : rawResponse.headers().entrySet()) {
                    response.getHeaders().put(entry.getKey(), entry.getValue());
                }
                if (rawResponse.body() != null) {
                    return new RawJsonBody(rawResponse.body());
                }
                return null;
            }
            return body;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
