window.onload = () => {
    let title = document.querySelector("#title");
    if (title) title.innerHTML = "Hi"
};

// TODO: clean me up.
export module Wirespec {
    export type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
    export type Content<T> = { type: string, body: T }
    export type Request<T> = {
        path: string,
        method: Method,
        query?: Record<string, any[]>,
        headers?: Record<string, any[]>,
        content?: Content<T>
    }
    export type Response<T> = { status: number, headers?: Record<string, any[]>, content?: Content<T> }
}

export module Todo {
    export const PATH = "/todo"
    export const METHOD = "GET"
    type RequestUndefined = { path: `/todo`, method: "GET", headers: { "auth": string }, query: { "done": boolean } }
    export type Request = RequestUndefined
    type Response200ApplicationJson = { status: 200, content: { type: "application/json", body: Todo } }
    type Response204Unit = { status: 204 }
    export type Response = Response200ApplicationJson | Response204Unit
    export type Handler = (request: Request) => Promise<Response>
    export type Call = {
        todo: Handler
    }
    export const requestUndefined = (props: { "done": boolean, "auth": string }) => ({
        path: `/todo`,
        method: "GET",
        query: {"done": props.done},
        headers: {"auth": props.auth}
    } as const)
    export const response200ApplicationJson = (props: { "body": Todo }) => ({
        status: 200,
        headers: {},
        content: {type: "application/json", body: props.body}
    } as const)

    // TODO: this response mapper should be part of the Wirespec core.
    // Note: FetchResponse is probably not what you want.
    // type UnknownResponse = { status: number, content: { type: string, body: unknown } }
    export const RESPONSE_MAPPER = async (fetchResponse: FetchResponse): Promise<Todo.Response> => {
        if (fetchResponse.status === 200 && fetchResponse.headers.get("Content-type") === "application/json") {
            return {
                status: fetchResponse.status,
                content: {
                    type: "application/json",
                    body: await fetchResponse.json()
                }
            }
        }
        if (fetchResponse.status === 204) {
            return {
                status: fetchResponse.status,
            }
        }
        throw new Error("Unknown response type")
    }

}

export type Todo = {
    "name": string,
    "done": boolean
}

type FetchResponse = Response
// let response: FetchResponse = await fetch("/url");
