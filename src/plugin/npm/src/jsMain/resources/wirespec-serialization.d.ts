export interface Serialization extends Serializer, Deserializer {}
export interface BodySerializer { serializeBody<T>(t: T, type: String): Uint8Array }
export interface BodyDeserializer { deserializeBody<T>(raw: Uint8Array, type: String): T }
export interface BodySerialization extends BodySerializer, BodyDeserializer {}
export interface PathSerializer { serializePath<T>(t: T, type: String): string }
export interface PathDeserializer { deserializePath<T>(raw: string, type: String): T }
export interface PathSerialization extends PathSerializer, PathDeserializer {}
export interface ParamSerializer { serializeParam<T>(value: T, type: String): string[] }
export interface ParamDeserializer { deserializeParam<T>(values: string[], type: String): T }
export interface ParamSerialization extends ParamSerializer, ParamDeserializer {}
export interface Serializer extends BodySerializer, PathSerializer, ParamSerializer {}
export interface Deserializer extends BodyDeserializer, PathDeserializer, ParamDeserializer {}

export const wirespecSerialization: Serialization