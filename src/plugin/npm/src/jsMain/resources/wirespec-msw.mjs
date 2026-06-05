import { http, HttpResponse } from 'msw'
import { wirespecSerialization } from './wirespec-serialization.mjs'

const METHODS = {
    GET: 'get',
    PUT: 'put',
    POST: 'post',
    DELETE: 'delete',
    PATCH: 'patch',
    HEAD: 'head',
    OPTIONS: 'options',
}

const PARAM = /^:(.+)$/
const BRACE = /^\{(.+)\}$/

const segments = (path) => path.replace(/^\/+/, '').split('/').filter((s) => s.length > 0)

// Normalize a Wirespec contract path into an MSW matcher: `{id}` -> `:id`, single leading slash.
const normalizePath = (path) => {
    const colon = path.replace(/\{([^/}]+)\}/g, ':$1')
    return colon.startsWith('/') ? colon : `/${colon}`
}

// Without a baseUrl, prefix the contract path with `*` so it matches on any origin
// (a bare relative path is not matched against absolute request URLs in Node). With a
// baseUrl, pin the origin/prefix instead.
const matcher = (baseUrl, path) => {
    const normalized = normalizePath(path)
    return baseUrl ? `${baseUrl.replace(/\/+$/, '')}${normalized}` : `*${normalized}`
}

// Rebuild the positional RawRequest.path from the contract template, substituting MSW's
// matched params for `:name`/`{name}` segments. Using the template (not the raw URL) keeps
// path-param indices aligned with the contract even when a baseUrl prefix is present.
const buildPath = (templatePath, params) =>
    segments(templatePath).map((segment) => {
        const match = segment.match(PARAM) ?? segment.match(BRACE)
        if (!match) return segment
        const value = params[match[1]]
        return Array.isArray(value) ? value[0] : value
    })

// The generated `from()` reads queries/headers as `Record<string, string[]>` (it pulls
// `values[0]` through `deserializeParam`), so collect every value per key into an array.
const readParams = (entries) => {
    const params = {}
    for (const [key, value] of entries) {
        if (params[key]) params[key].push(value)
        else params[key] = [value]
    }
    return params
}

// The generated `from()` reads the body as a `Uint8Array` (`deserializeBody` does
// `TextDecoder().decode(raw)`), so hand it the raw bytes rather than decoded text.
const readBody = async (request) => {
    if (request.method === 'GET' || request.method === 'HEAD') return undefined
    const buffer = await request.arrayBuffer()
    return buffer.byteLength > 0 ? new Uint8Array(buffer) : undefined
}

// The generated `to()` emits headers as `Record<string, string[]>`; flatten them into a
// `Headers` (each value appended) so they form a valid HeadersInit for MSW's HttpResponse.
const toHeaders = (rawHeaders) => {
    const headers = new Headers()
    for (const [key, values] of Object.entries(rawHeaders ?? {})) {
        for (const value of Array.isArray(values) ? values : [values]) {
            headers.append(key, String(value))
        }
    }
    return headers
}

/**
 * Build an MSW request handler for a generated Wirespec endpoint `api`.
 *
 * The resolver receives the deserialized, typed Wirespec request and must return one of
 * that endpoint's responses; a response from a different endpoint is a compile error.
 *
 * Options:
 *  - baseUrl: host or path prefix to prepend to the contract path (e.g. "https://api.example.com").
 *  - serialization: override the (de)serializer (defaults to wirespecSerialization).
 */
export function wirespec(api, resolver, options = {}) {
    const serialization = options.serialization ?? wirespecSerialization
    const pattern = matcher(options.baseUrl, api.path)
    const method = METHODS[String(api.method).toUpperCase()] ?? 'all'
    const endpoint = api.server(serialization)

    return http[method](pattern, async ({ request, params }) => {
        const url = new URL(request.url)
        const rawRequest = {
            method: request.method,
            path: buildPath(api.path, params),
            queries: readParams(url.searchParams),
            headers: readParams(request.headers),
            body: await readBody(request),
        }
        const typedRequest = endpoint.from(rawRequest)
        const response = await resolver(typedRequest)
        const rawResponse = endpoint.to(response)
        return new HttpResponse(rawResponse.body ?? null, {
            status: rawResponse.statusCode,
            headers: toHeaders(rawResponse.headers),
        })
    })
}
