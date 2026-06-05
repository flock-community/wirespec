import { HandleFetch, wirespecFetchIr } from "@flock/wirespec/fetch";
import { wirespecSerialization } from "@flock/wirespec/serialization";
import { expect, test, vi } from "vitest";
import { client } from "./gen/client";

const arrayBufferOf = (text: string) =>
  Promise.resolve(new TextEncoder().encode(text).buffer);

test("testGetTodoById", async () => {
  // @ts-ignore
  const mockHandler = vi.fn<HandleFetch>(() =>
    Promise.resolve({
      status: 200,
      headers: new Map([
        ["Content-Type", "application/json"],
        ["Content-Length", "2"],
      ]),
      arrayBuffer: () =>
        arrayBufferOf(`{"id": 123, "title": "test", "completed": false}`),
    }),
  );
  const apiClient = client(wirespecSerialization, {
    transport: (req) => wirespecFetchIr(req, mockHandler),
  });
  const res = await apiClient.getTodoById({ id: "123" });

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
      arrayBuffer: () =>
        arrayBufferOf(`[{"id": 123, "title": "test", "completed": false}]`),
    }),
  );
  const apiClient = client(wirespecSerialization, {
    transport: (req) => wirespecFetchIr(req, mockHandler),
  });
  const res = await apiClient.getTodos({ done: undefined });

  expect(res.status).toEqual(200);
  expect(res.headers).toEqual({ xTotal: "2" });
  expect(res.body).toEqual([
    {
      completed: false,
      id: 123,
      title: "test",
    },
  ]);
});
