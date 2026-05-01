export type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
export type RawRequest = { method: Method, path: string[], queries: Record<string, string>, headers: Record<string, string>, body?: string }
export type RawRequestIr = { method: string, path: string[], queries: Record<string, string[]>, headers: Record<string, string[]>, body: Uint8Array | undefined }
export type RawResponse = { status: number, headers: Record<string, string>, body?: string }
export type RawResponseIr = { statusCode:number, headers: Record<string, string[]>, body: Uint8Array | undefined }
export type HandleFetch = ( path:string, init?:RequestInit) => Promise<Response>

export declare function wirespecFetch  (rawRequest:(RawRequest), handle?: HandleFetch): Promise<RawResponse>
export declare function wirespecFetchIr  (rawRequest:(RawRequestIr), handle?: HandleFetch): Promise<RawResponseIr>