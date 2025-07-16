export declare const hello: string

export type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
export type RawRequest = { method: Method, path: string[], queries: Record<string, string>, headers: Record<string, string>, body?: string }
export type RawResponse = { status: number, headers: Record<string, string>, body?: string }
export type HandleFetch = ( path:string, init?:RequestInit) => Promise<Response>

export declare function wirespecFetch  (rawRequest:RawRequest, handle?: HandleFetch): Promise<RawResponse>