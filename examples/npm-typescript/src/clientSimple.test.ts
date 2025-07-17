import { GetTodoById, GetTodos, PostTodo } from "./gen/endpoint";
import { Wirespec } from "./gen/Wirespec";
import { test, expect } from "vitest";
import { wirespecSerialization } from 'wirespec/serialization'

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
    const rawRequest = client(wirespecSerialization).to(request);
    const rawResponse = mocks.find(it =>
        it.method === rawRequest.method &&
        it.path.join("/") === rawRequest.path.join("/")
    );
    if(rawResponse == undefined) throw new Error("Request is undefined")
    return Promise.resolve(client(wirespecSerialization).from(rawResponse));
};

const api: Api = {
    postTodo: handleFetch(PostTodo.client),
    getTodos: handleFetch(GetTodos.client),
    getTodoById: handleFetch(GetTodoById.client)
};

test('testGetTodos', async () => {
    const request: GetTodos.Request = GetTodos.request({done:undefined});
    const response = await api.getTodos(request);
    const expected = { status: 200, headers: {total:2}, body };
    expect(response).toEqual(expected)
})

test('testGetTodoById', async () => {
    const request: GetTodoById.Request = GetTodoById.request({ id: "1" });
    const response = await api.getTodoById(request);
    const expected = GetTodoById.response200({ body: body[0] });
    expect(response).toEqual(expected)
})

test('testPostTodo', async () => {
    const request: PostTodo.Request = {
        method: "POST",
        path: {},
        queries: {},
        headers: {},
        body: { name: "Do more", done: true }
    };
    const response = await api.postTodo(request);
    const expected = PostTodo.response200({ body: { id: "3", name: "Do more", done: true } });
    expect(response).toEqual(expected)
})
