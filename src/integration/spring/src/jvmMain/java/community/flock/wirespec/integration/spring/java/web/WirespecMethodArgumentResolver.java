package community.flock.wirespec.integration.spring.java.web;

import community.flock.wirespec.integration.spring.shared.UtilsKt;
import community.flock.wirespec.java.Wirespec;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static community.flock.wirespec.integration.spring.java.configuration.WirespecSerializationConfiguration.objectMapper;

public class WirespecMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private final Wirespec.Serialization wirespecSerialization;

    public WirespecMethodArgumentResolver(Wirespec.Serialization wirespecSerialization) {
        this.wirespecSerialization = wirespecSerialization;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Wirespec.Request.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Wirespec.Request<?> resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) throws Exception {
        HttpServletRequest servletRequest = (HttpServletRequest) webRequest.getNativeRequest();

        Class<?> declaringClass = parameter.getParameterType().getDeclaringClass();
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
             throw new IllegalStateException("Could not find Handlers class in " + declaringClass);
        }

        Wirespec.Server<?, ?> instance = (Wirespec.Server<?, ?>) handlersClass.getDeclaredConstructor().newInstance();
        Wirespec.RawRequest req = toRawRequest(servletRequest);
        return instance.getServer(wirespecSerialization).from(req);
    }

    private Wirespec.RawRequest toRawRequest(HttpServletRequest request) throws IOException {
        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)) {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            Map<String, List<MultipartFile>> multiFileMap = multipartRequest.getMultiFileMap();
            Map<String, Object> map = new HashMap<>();
            for (List<MultipartFile> files : multiFileMap.values()) {
                if (!files.isEmpty()) {
                    MultipartFile file = files.get(0);
                    String fileContentType = file.getContentType();
                    if (fileContentType == null) throw new IllegalStateException("No content type found for file " + file.getOriginalFilename());
                    MediaType mediaType = MediaType.valueOf(fileContentType);
                    byte[] bytes = file.getInputStream().readAllBytes();

                    if (MediaType.APPLICATION_JSON.equals(mediaType)) {
                        map.put(file.getName(), objectMapper.readTree(bytes));
                    } else {
                        map.put(file.getName(), bytes);
                    }
                }
            }

            return new Wirespec.RawRequest(
                request.getMethod(),
                UtilsKt.extractPath(request),
                UtilsKt.extractQueries(request),
                getHeadersMap(request),
                objectMapper.writeValueAsBytes(map)
            );
        }

        return new Wirespec.RawRequest(
            request.getMethod(),
            UtilsKt.extractPath(request),
            UtilsKt.extractQueries(request),
            getHeadersMap(request),
            request.getInputStream().readAllBytes()
        );
    }

    private Map<String, List<String>> getHeadersMap(HttpServletRequest request) {
        return Collections.list(request.getHeaderNames()).stream()
                .collect(Collectors.toMap(
                        name -> name,
                        name -> Collections.list(request.getHeaders(name))
                ));
    }
}
