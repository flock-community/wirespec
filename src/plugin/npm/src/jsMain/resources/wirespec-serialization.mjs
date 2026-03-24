const encoder = new TextEncoder();
const decoder = new TextDecoder();

export const wirespecSerialization = {
    deserialize(raw) {
        if (raw === undefined) {
            return undefined;
        }
        if (raw.startsWith('{') && raw.endsWith('}')) return JSON.parse(raw);
        if (raw.startsWith('[') && raw.endsWith(']')) return JSON.parse(raw);
        return raw;
    },
    serialize(type) {
        if (typeof type === 'string') {
            return type;
        }

        return JSON.stringify(type);
    },
    serializeBody(t, _type) {
        return encoder.encode(JSON.stringify(t));
    },
    deserializeBody(raw, _type) {
        return JSON.parse(decoder.decode(raw));
    },
    serializePath(t, _type) {
        return String(t);
    },
    deserializePath(raw, _type) {
        return raw;
    },
    serializeParam(value, _type) {
        return Array.isArray(value) ? value.map(String) : [String(value)];
    },
    deserializeParam(values, _type) {
        return values[0];
    },
};
