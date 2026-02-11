export type RawRequest = { method: string, path: string[], queries: Record<string, string[]>, headers: Record<string, string[]>, body: Uint8Array | undefined }
export type RawResponse = { statusCode: number, headers: Record<string, string[]>, body: Uint8Array | undefined  }
export type HandleFetch = ( path:string, init?:RequestInit) => Promise<Response>

export declare function wirespecFetch  (rawRequest:RawRequest, handle?: HandleFetch): Promise<RawResponse>