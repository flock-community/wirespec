package community.flock.wirespec.compiler.core.emit.shared

import community.flock.wirespec.compiler.core.emit.common.Emitter.Companion.SPACER

data object JavaShared : Shared {
    override val source = """
        |package community.flock.wirespec;
        |
        |import java.lang.reflect.Type;
        |import java.lang.reflect.ParameterizedType;
        |
        |public interface Wirespec {
        |${SPACER}interface Enum {};
        |${SPACER}interface Refined { String getValue(); };
        |${SPACER}interface Endpoint {};
        |${SPACER}enum Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE };
        |${SPACER}record Content<T> (String type, T body) {};
        |${SPACER}interface Request<T> { String getPath(); Method getMethod(); java.util.Map<String, java.util.List<Object>> getQuery(); java.util.Map<String, java.util.List<Object>> getHeaders(); Content<T> getContent(); }
        |${SPACER}interface Response<T> { int getStatus(); java.util.Map<String, java.util.List<Object>> getHeaders(); Content<T> getContent(); }
        |${SPACER}interface ContentMapper<B> { <T> Content<T> read(Content<B> content, Type valueType); <T> Content<B> write(Content<T> content); }
        |${SPACER}static Type getType(final Class<?> type, final boolean isIterable) {
        |$SPACER${SPACER}if(isIterable) {
        |$SPACER$SPACER${SPACER}return new ParameterizedType() {
        |$SPACER$SPACER$SPACER${SPACER}public Type getRawType() {return java.util.List.class;}
        |$SPACER$SPACER$SPACER${SPACER}public Type[] getActualTypeArguments() {Class<?>[] types = {type};return types;}
        |$SPACER$SPACER$SPACER${SPACER}public Type getOwnerType() {return null;}
        |$SPACER$SPACER$SPACER};
        |$SPACER$SPACER}
        |$SPACER${SPACER}else {
        |$SPACER$SPACER${SPACER}return type;
        |$SPACER$SPACER}
        |$SPACER}
        |}
    """.trimMargin()
}
