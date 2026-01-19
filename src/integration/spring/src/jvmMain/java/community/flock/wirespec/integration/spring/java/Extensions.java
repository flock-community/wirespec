package community.flock.wirespec.integration.spring.java;

import community.flock.wirespec.java.Wirespec;

import java.io.BufferedReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;

public class Extensions {

    @SuppressWarnings("unchecked")
    public static Wirespec.Request<?> invoke(
            Class<?> clazz,
            Method method,
            Wirespec.Serialization wirespecSerialization,
            Wirespec.Request<BufferedReader> request
    ) {
        try {
            Object func;
            if (isKotlinClass(clazz)) {
                Object companion = getKotlinCompanionObject(clazz);
                func = method.invoke(companion, wirespecSerialization);
            } else {
                func = method.invoke(null, wirespecSerialization);
            }
            
            if (func instanceof Function) {
                 return ((Function<Wirespec.Request<BufferedReader>, Wirespec.Request<?>>) func).apply(request);
            } else {
                 Method invokeMethod = func.getClass().getMethod("invoke", Object.class);
                 return (Wirespec.Request<?>) invokeMethod.invoke(func, request);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isKotlinClass(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredAnnotations())
                .anyMatch(a -> a.annotationType().getName().equals("kotlin.Metadata"));
    }

    private static Object getKotlinCompanionObject(Class<?> clazz) {
        try {
            java.lang.reflect.Field companionField = clazz.getDeclaredField("Companion");
            return companionField.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Could not find Companion object", e);
        }
    }
}
