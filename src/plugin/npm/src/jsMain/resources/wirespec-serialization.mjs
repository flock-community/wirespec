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
};