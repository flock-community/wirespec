function parsePrimitive(value, type) {
    const t = type.replace(/ \| undefined$/, '');
    switch (t) {
        case 'number':
        case 'integer':
            return Number(value);
        case 'boolean':
            return value === 'true';
        default:
            return value;
    }
}

export const wirespecSerialization = {
    serializeBody(t, type) {
        if (t === undefined) {
            return undefined;
        }
        const json = JSON.stringify(t);
        return new TextEncoder().encode(json);
    },
    deserializeBody(raw, type) {
        if (raw === undefined) {
            return undefined;
        }
        const text = typeof raw === 'string' ? raw : new TextDecoder().decode(raw);
        try {
            return JSON.parse(text);
        } catch {
            return text;
        }
    },
    serializePath(t, type) {
        return String(t);
    },
    deserializePath(raw, type) {
        return parsePrimitive(raw, type);
    },
    serializeParam(value, type) {
        if (Array.isArray(value)) {
            return value.map(v => String(v));
        }
        return [String(value)];
    },
    deserializeParam(values, type) {
        const t = type.replace(/ \| undefined$/, '');
        if (t.endsWith('[]')) {
            const elementType = t.slice(0, -2);
            return values.map(v => parsePrimitive(v, elementType));
        }
        return parsePrimitive(values[0], t);
    },
};
