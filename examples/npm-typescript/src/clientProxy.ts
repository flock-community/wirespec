import { GetTodoById, GetTodos, PostTodo, Wirespec } from "./gen/Todo";
import * as assert from "node:assert";
import Client = Wirespec.Client;

const serialization: Wirespec.Serialization = {
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

const mock = (method: Wirespec.Method, path: string[], status: number, headers: Record<string, string>, body: any) => ({
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

type UnionToIntersection<U> = (U extends any ? (k: U) => void : never) extends (k: infer I) => void ? I : never;
type Api<Han extends any> =  Wirespec.Api<Wirespec.Request<any>, Wirespec.Response<any>, Han>
type WebClient = <Apis extends Api<any>[]>(...apis: Apis) => UnionToIntersection<Apis[number] extends Api<infer Han> ? Han : never>;

const webClient:WebClient = (...apis) => {
    const activeClients:Record<string, ReturnType<Client<Wirespec.Request<any>, Wirespec.Response<any>, any>>> = apis.reduce((acc, cur) => ({...acc, [cur.name] : cur.client(serialization)}), {})
    const proxy = new Proxy({}, {
        get: (_, prop) => {
            const client = activeClients[prop as keyof typeof activeClients];
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
    return proxy as ReturnType<WebClient>
}

const api = webClient(PostTodo.api, GetTodos.api, GetTodoById.api)

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
