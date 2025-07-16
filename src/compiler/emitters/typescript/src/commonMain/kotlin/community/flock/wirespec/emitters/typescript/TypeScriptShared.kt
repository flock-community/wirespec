package community.flock.wirespec.emitters.typescript

import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.Shared
import community.flock.wirespec.compiler.core.emit.Spacer

data object TypeScriptShared : Shared {
    override val packageString: String = DEFAULT_SHARED_PACKAGE_STRING

    override val source = """
        |export namespace Wirespec {
        |${Spacer}export type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
        |${Spacer}export type RawRequest = { method: Method, path: string[], queries: Record<string, string>, headers: Record<string, string>, body?: string }
        |${Spacer}export type RawResponse = { status: number, headers: Record<string, string>, body?: string }
        |${Spacer}export type Request<T> = { path: Record<string, unknown>, method: Method, queries?: Record<string, unknown>, headers?: Record<string, unknown>, body?:T }
        |${Spacer}export type Response<T> = { status:number, headers?: Record<string, unknown>, body?:T }
        |${Spacer}export type Serialization = { serialize: <T>(type: T) => string; deserialize: <T>(raw: string | undefined) => T }
        |${Spacer}export type Client<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => { to: (request: REQ) => RawRequest; from: (response: RawResponse) => RES }
        |${Spacer}export type Server<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => { from: (request: RawRequest) => REQ; to: (response: RES) => RawResponse }
        |${Spacer}export type Api<REQ extends Request<unknown>, RES extends Response<unknown>> = { name: string; method: Method, path: string, client: Client<REQ, RES>; server: Server<REQ, RES> }
        |}
    """.trimMargin()
}
