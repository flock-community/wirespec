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
