import {GetTodoById, GetTodos, PostTodo, Wirespec} from "./gen/Todo";
import * as assert from "node:assert";

const serialization: Wirespec.Serialization = {
    deserialize<T>(raw: string | undefined): T {
        if (raw) {
            return JSON.parse(raw) as T;
        } else {
            return undefined
        }
    },
    serialize<T>(type: T): string {
        return JSON.stringify(type);
    }
}

const body = [
    {id: "1", name: "Do it now", done: true},
    {id: "2", name: "Do it tomorrow", done: false},
]

type Api =
    GetTodos.Handler &
    GetTodoById.Handler &
    PostTodo.Handler

const api: Api = {
    postTodo(request: PostTodo.Request): Promise<PostTodo.Response> {
        return Promise.resolve(PostTodo.response200({
            body: {
                id: "3",
                ...request.body
            }
        }));
    },
    getTodos(_: GetTodos.Request): Promise<GetTodos.Response> {
        return Promise.resolve(GetTodos.response200({body}));
    },
    getTodoById(_: GetTodoById.Request): Promise<GetTodoById.Response> {
        return Promise.resolve(GetTodoById.response200({body: body[0]}));
    }
}

const testGetTodos = async () => {
    const rawRequest: Wirespec.RawRequest = {
        method: "GET",
        path: ["todos"],
        queries: {},
        headers: {},
    }
    const server = GetTodos.server(serialization)
    const request = server.from(rawRequest)
    const response = await api.getTodos(request)
    const rawResponse = server.to(response)
    const expected = {status: 200, headers: {}, body: JSON.stringify(body)}
    assert.deepEqual(rawResponse, expected)
}

const testGetTodoById = async () => {
    const rawRequest: Wirespec.RawRequest = {
        method: "GET",
        path: ["todos", "1"],
        queries: {},
        headers: {},
    }
    const server = GetTodoById.server(serialization)
    const request = server.from(rawRequest)
    const response = await api.getTodoById(request)
    const rawResponse = server.to(response)
    const expected = {status: 200, headers: {}, body: JSON.stringify(body[0])}
    assert.deepEqual(rawResponse, expected)
}

const testPostTodo = async () => {
    const rawRequest: Wirespec.RawRequest = {
        method: "GET",
        path: ["todos"],
        queries: {},
        headers: {},
        body: JSON.stringify({name: "Do it later", done: false})
    }
    const server = PostTodo.server(serialization)
    const request = server.from(rawRequest)
    const response = await api.postTodo(request)
    const rawResponse = server.to(response)
    const expected = {status: 200, headers: {}, body: JSON.stringify({id: "3", name: "Do it later", done: false})}
    assert.deepEqual(rawResponse, expected)
}

Promise.all([
    testGetTodos(),
    testGetTodoById(),
    testPostTodo(),
])
