package community.flock.wirespec.integration.spring.java.web;

import community.flock.wirespec.integration.spring.shared.UtilsKt;
import community.flock.wirespec.integration.spring.shared.WirespecJsonMapper;
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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WirespecMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private final Wirespec.Serialization wirespecSerialization;
    private final WirespecJsonMapper jsonMapper;
    private final Map<Class<?>, Method> fromRequestCache = new ConcurrentHashMap<>();

    public WirespecMethodArgumentResolver(Wirespec.Serialization wirespecSerialization, WirespecJsonMapper jsonMapper) {
        this.wirespecSerialization = wirespecSerialization;
        this.jsonMapper = jsonMapper;
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
        Method fromRequest = fromRequestCache.computeIfAbsent(
                declaringClass,
                cls -> findStaticFactory(cls, "fromRawRequest", "fromRequest"));

        Wirespec.RawRequest req = toRawRequest(servletRequest);
        return (Wirespec.Request<?>) fromRequest.invoke(null, wirespecSerialization, req);
    }

    /**
     * Locates the static factory method on an endpoint class, tolerating both emitter shapes:
     * the IR emitter declares it on the enclosing endpoint class, while the legacy emitter
     * declares it on the nested {@code Handler} interface.
     */
    static Method findStaticFactory(Class<?> endpointClass, String rawName, String legacyName) {
        List<Method> candidates = new ArrayList<>(Arrays.asList(endpointClass.getDeclaredMethods()));
        Arrays.stream(endpointClass.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equals("Handler"))
                .findFirst()
                .ifPresent(handler -> candidates.addAll(Arrays.asList(handler.getDeclaredMethods())));
        return candidates.stream()
                .filter(m -> (m.getName().equals(rawName) || m.getName().equals(legacyName)) && Modifier.isStatic(m.getModifiers()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(rawName + " method not found in " + endpointClass + " or its Handler"));
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
                        map.put(file.getName(), jsonMapper.readTree(bytes));
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
                java.util.Optional.of(jsonMapper.writeValueAsBytes(map))
            );
        }

        return new Wirespec.RawRequest(
            request.getMethod(),
            UtilsKt.extractPath(request),
            UtilsKt.extractQueries(request),
            getHeadersMap(request),
            java.util.Optional.of(request.getInputStream().readAllBytes())
        );
    }

    private Map<String, List<String>> getHeadersMap(HttpServletRequest request) {
        return Collections.list(request.getHeaderNames()).stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        name -> Collections.list(request.getHeaders(name))
                ));
    }
}
