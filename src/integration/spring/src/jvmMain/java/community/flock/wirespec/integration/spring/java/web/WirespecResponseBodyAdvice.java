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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ControllerAdvice
public class WirespecResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final Wirespec.Serialization wirespecSerialization;
    private final Map<Class<?>, Method> toResponseCache = new ConcurrentHashMap<>();

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
            Method toResponse = toResponseCache.computeIfAbsent(declaringClass, cls -> {
                Class<?> handlerClass = Arrays.stream(cls.getDeclaredClasses())
                        .filter(c -> c.getSimpleName().equals("Handler"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Handler not found in " + cls));
                return Arrays.stream(handlerClass.getDeclaredMethods())
                        .filter(m -> m.getName().equals("toResponse") && Modifier.isStatic(m.getModifiers()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("toResponse method not found in " + handlerClass));
            });

            if (body instanceof Wirespec.Response<?> wirespecResponse) {
                Wirespec.RawResponse rawResponse = (Wirespec.RawResponse) toResponse.invoke(null, wirespecSerialization, wirespecResponse);
                
                response.setStatusCode(HttpStatusCode.valueOf(rawResponse.statusCode()));
                for (Map.Entry<String, List<String>> entry : rawResponse.headers().entrySet()) {
                    response.getHeaders().put(entry.getKey(), entry.getValue());
                }
                return rawResponse.body().map(RawJsonBody::new).orElse(null);
            }
            return body;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
