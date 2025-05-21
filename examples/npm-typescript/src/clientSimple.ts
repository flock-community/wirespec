import { GetTodoById, GetTodos, PostTodo } from "./gen/endpoint";
import { Wirespec } from "./gen/Wirespec";
import * as assert from "node:assert";

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

type Api =
    GetTodos.Handler &
    GetTodoById.Handler &
    PostTodo.Handler

const handleFetch = <Req extends Wirespec.Request<any>, Res extends Wirespec.Response<any>>(client: Wirespec.Client<Req, Res>) => (request: Req): Promise<Res> => {
    const mock = (method: Wirespec.Method, path: string[], status: number, headers: Record<string, string>, body: any) => ({
        method,
        path,
        status,
        headers,
        body
    });
    const mocks = [
        mock("GET", ["api", "todos"], 200, {total:"2"}, JSON.stringify(body)),
        mock("GET", ["api", "todos", "1"], 200, {}, JSON.stringify(body[0])),
        mock("POST", ["api", "todos"], 200, {}, JSON.stringify({ id: "3", name: "Do more", done: true }))
    ];
    const rawRequest = client(serialization).to(request);
    const rawResponse: Wirespec.RawResponse = mocks.find(it =>
        it.method === rawRequest.method &&
        it.path.join("/") === rawRequest.path.join("/")
    );
    assert.notEqual(rawResponse, undefined);
    return Promise.resolve(client(serialization).from(rawResponse));
};

const api: Api = {
    postTodo: handleFetch(PostTodo.client),
    getTodos: handleFetch(GetTodos.client),
    getTodoById: handleFetch(GetTodoById.client)
};

const testGetTodos = async () => {
    const request: GetTodos.Request = GetTodos.request({done:undefined});
    const response = await api.getTodos(request);
    const expected = { status: 200, headers: {total:2}, body };
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
