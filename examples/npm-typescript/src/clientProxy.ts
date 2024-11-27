import {GetTodoById, GetTodos, PostTodo, Wirespec} from "./gen/Todo";
import {GetUsers} from "./gen/User";
import * as assert from "node:assert";

type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
type RawRequest = { method: Method, path: string[], queries: Record<string, string>, headers: Record<string, string>, body?: string }
type RawResponse = { status: number, headers: Record<string, string>, body?: string }
type Content<T> = { type:string, body:T }
type Request<T> = { path: Record<string, unknown>, method: Method, query?: Record<string, unknown>, headers?: Record<string, unknown>, content?:Content<T> }
type Response<T> = { status:number, headers?: Record<string, unknown[]>, content?:Content<T> }
type Serialization = { serialize: <T>(type: T) => string; deserialize: <T>(raw: string | undefined) => T }
type Client<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => { to: (request: REQ) => RawRequest; from: (response: RawResponse) => RES }
type Server<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => { from: (request: RawRequest) => REQ; to: (response: RES) => RawResponse }
type Api<REQ extends Request<unknown>, RES extends Response<unknown>> = { name: string; method: Method, path: string, client: Client<REQ, RES>; server: Server<REQ, RES> }

const serialization: Serialization = {
    deserialize<T>(raw: string | undefined): T {
        if (raw === undefined) {
            return undefined;
        } else {
            return JSON.parse(raw) as T;
        }
    },
    serialize<T>(type: T): string {
        if (typeof type === "string") {
            return type;
        } else {
            return JSON.stringify(type);
        }
    }
};

const body = [
    { id: "1", name: "Do it now", done: true },
    { id: "2", name: "Do it tomorrow", done: false }
];

const mock = (method: Method, path: string[], status: number, headers: Record<string, string>, body: any) => ({
    method,
    path,
    status,
    headers,
    body
});

const mocks = [
    mock("GET", ["api", "todos"], 200, {}, JSON.stringify(body)),
    mock("GET", ["api", "todos", "1"], 200, {}, JSON.stringify(body[0])),
    mock("POST", ["api", "todos"], 200, {}, JSON.stringify({ id: "3", name: "Do more", done: true }))
];

type ApiClient<REQ, RES> = (req: REQ) => Promise<RES>;
type WebClient = <Apis extends Api<Request<unknown>, Response<unknown>>[]>(...apis: Apis) => {
    [K in Apis[number]['name']]: Extract<Apis[number], { name: K }> extends Api<infer ReqType, infer ResType> ?
        ApiClient<ReqType, ResType> : never
};


const webClient:WebClient = (...apis) => {
    const proxy = new Proxy({}, {
        get: (_, prop) => {
            const api = apis.find(it => it.name === prop);
            const client = api.client(serialization);
            return (req:Wirespec.Request<unknown>) => {
                const rawRequest = client.to(req);
                const rawResponse: Wirespec.RawResponse = mocks.find(it =>
                    it.method === rawRequest.method &&
                    it.path.join("/") === rawRequest.path.join("/")
                );
                assert.notEqual(rawResponse, undefined);
                return Promise.resolve(client.from(rawResponse));
            }

        },
    });
    return proxy as any
}

const api = webClient(PostTodo.api, GetTodos.api, GetTodoById.api, GetUsers.api)

const testGetTodos = async () => {
    const request: GetTodos.Request = GetTodos.request();
    const response = await api.getTodos(request);
    const expected = { status: 200, headers: {}, body };
    assert.deepEqual(response, expected);
};

const testGetTodoById = async () => {
    const request: GetTodoById.Request = GetTodoById.request({ id: "1" });
    const response = await api.getTodoById(request);
    const expected = GetTodoById.response200({ body: body[0] });
    assert.deepEqual(response, expected);
};

const testPostTodo = async () => {
    const request: PostTodo.Request = {
        method: "POST",
        path: {},
        queries: {},
        headers: {},
        body: { name: "Do more", done: true }
    };
    const response = await api.postTodo(request);
    const expected = PostTodo.response200({ body: { id: "3", name: "Do more", done: true } });
    assert.deepEqual(response, expected);
};

Promise.all([
    testGetTodos(),
    testGetTodoById(),
    testPostTodo()
]);
