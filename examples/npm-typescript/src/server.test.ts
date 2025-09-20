import { wirespecSerialization } from "@flock/wirespec/serialization";
import { expect, test } from "vitest";
import { Wirespec } from "./gen/Wirespec";
import { GetTodoById, GetTodos, PostTodo } from "./gen/endpoint";

const body = [
  { id: "1", name: "Do it now", done: true },
  { id: "2", name: "Do it tomorrow", done: false },
];

type Api = GetTodos.Handler & GetTodoById.Handler & PostTodo.Handler;

const api: Api = {
  postTodo(request: PostTodo.Request): Promise<PostTodo.Response> {
    return Promise.resolve(
      PostTodo.response200({
        body: {
          id: "3",
          ...request.body,
        },
      })
    );
  },
  getTodos(_: GetTodos.Request): Promise<GetTodos.Response> {
    return Promise.resolve(GetTodos.response200({ "X-Total": 2, body }));
  },
  getTodoById(_: GetTodoById.Request): Promise<GetTodoById.Response> {
    return Promise.resolve(GetTodoById.response200({ body: body[0] }));
  },
};

test("testGetTodos", async () => {
  const rawRequest: Wirespec.RawRequest = {
    method: "GET",
    path: ["todos"],
    queries: {},
    headers: {},
  };
  const server = GetTodos.server(wirespecSerialization);
  const request = server.from(rawRequest);
  const response = await api.getTodos(request);
  const rawResponse = server.to(response);
  const expected = {
    status: 200,
    headers: { "X-Total": "2" },
    body: JSON.stringify(body),
  };

  expect(rawResponse).toEqual(expected);
});

test("testGetTodoById", async () => {
  const rawRequest: Wirespec.RawRequest = {
    method: "GET",
    path: ["todos", "1"],
    queries: {},
    headers: {},
  };
  const server = GetTodoById.server(wirespecSerialization);
  const request = server.from(rawRequest);
  const response = await api.getTodoById(request);
  const rawResponse = server.to(response);
  const expected = { status: 200, headers: {}, body: JSON.stringify(body[0]) };

  expect(rawResponse).toEqual(expected);
});

test("testPostTodo", async () => {
  const rawRequest: Wirespec.RawRequest = {
    method: "GET",
    path: ["todos"],
    queries: {},
    headers: {},
    body: JSON.stringify({ name: "Do it later", done: false }),
  };
  const server = PostTodo.server(wirespecSerialization);
  const request = server.from(rawRequest);
  const response = await api.postTodo(request);
  const rawResponse = server.to(response);
  const expected = {
    status: 200,
    headers: {},
    body: JSON.stringify({ id: "3", name: "Do it later", done: false }),
  };

  expect(rawResponse).toEqual(expected);
});
