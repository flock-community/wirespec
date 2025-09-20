import { wirespecSerialization } from "@flock/wirespec/serialization";
import * as assert from "node:assert";
import { expect, test } from "vitest";
import { Wirespec } from "./gen/Wirespec";
import { GetTodoById, GetTodos, GetUsers, PostTodo } from "./gen/endpoint";

const body = [
  { id: "1", name: "Do it now", done: true },
  { id: "2", name: "Do it tomorrow", done: false },
];

const mock = (
  method: Wirespec.Method,
  path: string[],
  status: number,
  headers: Record<string, string>,
  body: any,
) => ({
  method,
  path,
  status,
  headers,
  body,
});

const mocks = [
  mock("GET", ["api", "todos"], 200, { "X-Total": "2" }, JSON.stringify(body)),
  mock("GET", ["api", "todos", "1"], 200, {}, JSON.stringify(body[0])),
  mock(
    "POST",
    ["api", "todos"],
    200,
    {},
    JSON.stringify({ id: "3", name: "Do more", done: true }),
  ),
];

type ApiClient<REQ, RES> = (req: REQ) => Promise<RES>;
type WebClient = <Apis extends Wirespec.Api<any, any>[]>(
  ...apis: Apis
) => {
  [K in Apis[number]["name"]]: Extract<
    Apis[number],
    { name: K }
  > extends Wirespec.Api<infer Req, infer Res>
    ? ApiClient<Req, Res>
    : never;
};

const webClient: WebClient = (...apis) => {
  const proxy = new Proxy(
    {},
    {
      get: (_, prop) => {
        const api = apis.find((it) => it.name === prop);
        if (api == undefined)
          throw new Error(`Api not found ${prop.toString()}`);
        const client = api.client(wirespecSerialization);
        return (req: Wirespec.Request<unknown>) => {
          const rawRequest = client.to(req);
          const rawResponse = mocks.find(
            (it) =>
              it.method === rawRequest.method &&
              it.path.join("/") === rawRequest.path.join("/"),
          );
          if (rawResponse == undefined) throw new Error("Request is undefined");
          assert.notEqual(rawResponse, undefined);
          return Promise.resolve(client.from(rawResponse));
        };
      },
    },
  );
  return proxy as any;
};

const api = webClient(
  PostTodo.api,
  GetTodos.api,
  GetTodoById.api,
  GetUsers.api,
);

test("testGetTodos", async () => {
  const request: GetTodos.Request = GetTodos.request({});
  const response = await api.getTodos(request);
  const expected = { status: 200, headers: { "X-Total": "2" }, body };

  expect(response).toEqual(expected);
});

test("testGetTodoById", async () => {
  const request: GetTodoById.Request = GetTodoById.request({ id: "1" });
  const response = await api.getTodoById(request);
  const expected = GetTodoById.response200({ body: body[0] });

  expect(response).toEqual(expected);
});

test("testPostTodo", async () => {
  const request: PostTodo.Request = {
    method: "POST",
    path: {},
    queries: {},
    headers: {},
    body: { name: "Do more", done: true },
  };
  const response = await api.postTodo(request);
  const expected = PostTodo.response200({
    body: { id: "3", name: "Do more", done: true },
  });

  expect(response).toEqual(expected);
});
