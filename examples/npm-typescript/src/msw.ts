// Example: mock a Wirespec API with Mock Service Worker (MSW) using @flock/wirespec/msw.
//
// `wirespec(Endpoint.api, resolver)` turns a generated endpoint into a typed MSW
// RequestHandler: the resolver receives the deserialized Wirespec request and must
// return one of that endpoint's responses (a response from another endpoint is a
// compile error).
import { setupServer } from "msw/node";
import { wirespec } from "@flock/wirespec/msw";
import { GetTodoById, GetTodos, PostTodo } from "./gen/endpoint";
import type { TodoDto } from "./gen/model";

export const todo: TodoDto = {
  id: "12345678-1234-1234-1234-123456789abc",
  name: "Buy milk",
  done: false,
  testInt0: 0,
  testInt1: 1,
  testInt2: 3,
  testNum0: 0.0,
  testNum1: 1.1,
  testNum2: 3.5,
};

export const handlers = [
  wirespec(GetTodos.api, async () =>
    GetTodos.response200({ xTotal: 1, body: [todo] }),
  ),

  wirespec(GetTodoById.api, async (req) =>
    req.path.id === todo.id
      ? GetTodoById.response200({ body: todo })
      : GetTodoById.response404({
          body: { code: 404, description: "not found" },
        }),
  ),

  wirespec(PostTodo.api, async (req) =>
    PostTodo.response200({ body: { id: todo.id, ...req.body } }),
  ),
];

export const server = setupServer(...handlers);
