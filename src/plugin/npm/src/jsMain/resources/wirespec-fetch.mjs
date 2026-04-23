export async function wirespecFetch(req, handler) {
    const contentHeader = req.body ? { 'Content-Type': 'application/json' } : {};
    const body = req.body !== undefined ? req.body : undefined;
    const query = Object.entries(req.queries)
        .filter(([_, value]) => value !== undefined)
        .flatMap(([key, value]) => {
            if (value && typeof value === 'string' && value.startsWith('[') && value.endsWith(']')) {
                const parsedValue = JSON.parse(value);
                if (Array.isArray(parsedValue)) {
                    return parsedValue.map((item) => `${key}=${item}`);
                }
            }
            return `${key}=${value}`;
        })
        .join('&');
    const path = req.path
        .map(segment => encodeURIComponent(segment))
        .join('/')
    const url = `/${path}${query ? `?${query}` : ''}`;
    const init = {method: req.method, body, headers: {...req.headers, ...contentHeader}}
    const res = handler ? await handler(url, init) : await fetch(url, init)
    const contentType = res.headers.get('Content-Type');
    const contentLength = res.headers.get('Content-Length');
    return {
        status: res.status,
        headers: {
            ...[...res.headers.entries()].reduce((acc, [key, value]) => ({...acc, [key]: value}), {}),
            'Content-Type': contentType,
        },
        body: contentLength !== '0' && contentType ? await res.text() : undefined,
    };

}

export async function wirespecFetchIr(req, handler) {
    const contentHeader = req.body ? { 'Content-Type': 'application/json' } : {};
    const query = Object.entries(req.queries)
        .filter(([, value]) => value !== undefined)
        .flatMap(([key, values]) =>
            Array.isArray(values)
                ? values.map((v) => `${encodeURIComponent(key)}=${encodeURIComponent(v)}`)
                : [`${encodeURIComponent(key)}=${encodeURIComponent(values)}`])
        .join('&');
    const path = req.path
        .map(segment => encodeURIComponent(segment))
        .join('/')
    const url = `/${path}${query ? `?${query}` : ''}`;
    const init = {method: req.method, body: req.body, headers: {...req.headers, ...contentHeader}}
    const res = handler ? await handler(url, init) : await fetch(url, init)
    const headers = [...res.headers.entries()]
        .reduce((acc, [key, value]) => ({...acc, [key]: [value]}), {});
    const hasBody = ![204, 205, 304].includes(res.status);
    const buf = hasBody ? await res.arrayBuffer() : undefined;
    const body = buf && buf.byteLength > 0 ? new Uint8Array(buf) : undefined;
    return {
        statusCode: res.status,
        headers,
        body,
    };

}