export type Type = string

export interface Serialization {
    serialize<T>(typed: T): string;
    deserialize<T>(raw: string | undefined): T;
    serializeBody<T>(t: T, type: Type): Uint8Array;
    deserializeBody<T>(raw: Uint8Array, type: Type): T;
    serializePath<T>(t: T, type: Type): string;
    deserializePath<T>(raw: string, type: Type): T;
    serializeParam<T>(value: T, type: Type): string[];
    deserializeParam<T>(values: string[], type: Type): T;
}

export declare const wirespecSerialization: Serialization
