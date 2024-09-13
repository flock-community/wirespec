package community.flock.wirespec.compiler.core.emit.shared

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Spacer

data object JavaLegacyShared : Shared {
    override val source = """
        |package $DEFAULT_SHARED_PACKAGE_STRING.java;
        |
        |import java.lang.reflect.Type;
        |import java.lang.reflect.ParameterizedType;
        |
        |public interface Wirespec {
        |${Spacer}interface Enum {};
        |${Spacer}interface Refined { String getValue(); };
        |${Spacer}interface Endpoint {};
        |${Spacer}enum Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE };
        |${Spacer}record Content<T> (String type, T body) {};
        |${Spacer}interface Request<T> { String getPath(); Method getMethod(); java.util.Map<String, java.util.List<Object>> getQuery(); java.util.Map<String, java.util.List<Object>> getHeaders(); Content<T> getContent(); }
        |${Spacer}interface Response<T> { int getStatus(); java.util.Map<String, java.util.List<Object>> getHeaders(); Content<T> getContent(); }
        |${Spacer}interface ContentMapper<B> { <T> Content<T> read(Content<B> content, Type valueType); <T> Content<B> write(Content<T> content); }
        |${Spacer}static Type getType(final Class<?> type, final boolean isIterable) {
        |$Spacer${Spacer}if(isIterable) {
        |$Spacer$Spacer${Spacer}return new ParameterizedType() {
        |$Spacer$Spacer$Spacer${Spacer}public Type getRawType() {return java.util.List.class;}
        |$Spacer$Spacer$Spacer${Spacer}public Type[] getActualTypeArguments() {Class<?>[] types = {type};return types;}
        |$Spacer$Spacer$Spacer${Spacer}public Type getOwnerType() {return null;}
        |$Spacer$Spacer$Spacer};
        |$Spacer$Spacer}
        |$Spacer${Spacer}else {
        |$Spacer$Spacer${Spacer}return type;
        |$Spacer$Spacer}
        |$Spacer}
        |}
    """.trimMargin()
}
