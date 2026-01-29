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
            Class<?> endpointClass = returnType.getParameterType().getDeclaringClass();
            if (body instanceof Wirespec.Response<?> wirespecResponse) {
                final Class<?> adapterClass = Arrays.stream(endpointClass.getDeclaredClasses())
                        .filter(c -> c.getSimpleName().equals("Adapter"))
                        .findFirst()
                        .orElseThrow();
                final Class<?> responseClass = Arrays.stream(endpointClass.getClasses())
                        .filter(Wirespec.Response.class::isAssignableFrom)
                        .filter(c -> c.getSimpleName().equals("Response"))
                        .findFirst().orElseThrow();
                final Method toRawRequestMethod = adapterClass.getMethod("toRawResponse", Wirespec.Serializer.class, responseClass);
                final Wirespec.RawResponse rawResponse = (Wirespec.RawResponse) toRawRequestMethod.invoke(null, this.wirespecSerialization, wirespecResponse);
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
