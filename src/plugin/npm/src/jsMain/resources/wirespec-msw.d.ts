import type { RequestHandler } from 'msw'
import type { Serialization } from './wirespec-serialization'

// Mirrors the generated `Wirespec.RawRequest` / `RawResponse` so generated endpoint `api`
// objects are assignable to WirespecMswEndpoint (the method union must match exactly).
type Method = 'GET' | 'PUT' | 'POST' | 'DELETE' | 'OPTIONS' | 'HEAD' | 'PATCH' | 'TRACE'

type RawRequest = {
    method: Method
    path: string[]
    queries: Record<string, string>
    headers: Record<string, string>
    body?: string
}

type RawResponse = {
    status: number
    headers: Record<string, string>
    body?: string
}

/**
 * Structural mirror of the generated `Wirespec.Api<Req, Res>` type. The shipped module
 * cannot import the consumer's per-project generated `Wirespec` namespace, so the shape
 * is redeclared here; generated endpoint `api` objects are assignable to it.
 */
export type WirespecMswEndpoint<Req, Res> = {
    method: string
    path: string
    server: (serialization: Serialization) => {
        from: (request: RawRequest) => Req
        to: (response: Res) => RawResponse
    }
}

export type WirespecMswOptions = {
    /** Host or path prefix prepended to the contract path, e.g. "https://api.example.com" or "/api". */
    baseUrl?: string
    /** Override the serialization used to (de)serialize requests/responses. */
    serialization?: Serialization
}

/**
 * Build a typed MSW request handler for a generated Wirespec endpoint.
 *
 * The generics are inferred from `api.server`, constraining `resolver` to the endpoint's
 * own request/response types: returning a response from a different endpoint is a compile error.
 */
export declare function wirespec<Req, Res>(
    api: WirespecMswEndpoint<Req, Res>,
    resolver: (request: Req) => Res | Promise<Res>,
    options?: WirespecMswOptions,
): RequestHandler
