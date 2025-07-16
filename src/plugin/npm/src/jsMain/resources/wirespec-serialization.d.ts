export type Serialization = { serialize: <T>(typed: T) => string; deserialize: <T>(raw: string | undefined) => T }

export const wirespecSerialization: Serialization