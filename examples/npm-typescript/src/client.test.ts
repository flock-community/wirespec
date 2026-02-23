import { HandleFetch, wirespecFetch } from "@flock/wirespec/fetch";
import { wirespecSerialization } from "@flock/wirespec/serialization";
import { expect, test, vi } from "vitest";
import { client } from "./gen/client";

test("testGetTodoById", async () => {
  // @ts-ignore
  const mockHandler = vi.fn<HandleFetch>(() =>
    Promise.resolve({
      status: 200,
      headers: new Map([
        ["Content-Type", "application/json"],
        ["Content-Length", "2"],
      ]),
      text: () =>
        Promise.resolve(`{"id": 123, "title": "test", "completed": false}`),
    }),
  );
  const apiClient = client(wirespecSerialization, (req) =>
    wirespecFetch(req, mockHandler),
  );
  const res = await apiClient.GetTodoById({ id: "123" });

  expect(res.status).toEqual(200);
  expect(res.headers).toEqual({});
  expect(res.body).toEqual({
    completed: false,
    id: 123,
    title: "test",
  });
});

test("testGetTodos", async () => {
  // @ts-ignore
  const mockHandler = vi.fn<HandleFetch>(() =>
    Promise.resolve({
      status: 200,
      headers: new Map([
        ["Content-Type", "application/json"],
        ["Content-Length", "2"],
        ["X-Total", "2"],
      ]),
      text: () =>
        Promise.resolve(`[{"id": 123, "title": "test", "completed": false}]`),
    }),
  );
  const apiClient = client(wirespecSerialization, (req) =>
    wirespecFetch(req, mockHandler),
  );
  const res = await apiClient.GetTodos({ done: undefined });

  expect(res.status).toEqual(200);
  expect(res.headers).toEqual({ "X-Total": "2" });
  expect(res.body).toEqual([
    {
      completed: false,
      id: 123,
      title: "test",
    },
  ]);
});
