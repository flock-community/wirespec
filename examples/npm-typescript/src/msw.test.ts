import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, expect, test } from "vitest";
import { wirespec } from "@flock/wirespec/msw";
import { GetTodos } from "./gen/endpoint";
import { server, todo } from "./msw";

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

test("GET /api/todos returns the typed list and response header", async () => {
  const res = await fetch("http://localhost/api/todos");

  expect(res.status).toBe(200);
  expect(res.headers.get("X-Total")).toBe("1");
  expect(await res.json()).toEqual([todo]);
});

test("GET /api/todos/:id resolves the path param to a 200", async () => {
  const res = await fetch(`http://localhost/api/todos/${todo.id}`);

  expect(res.status).toBe(200);
  expect(await res.json()).toEqual(todo);
});

test("GET /api/todos/:id returns a typed 404 for an unknown id", async () => {
  const res = await fetch(
    "http://localhost/api/todos/00000000-0000-0000-0000-000000000000",
  );

  expect(res.status).toBe(404);
  expect(await res.json()).toEqual({ code: 404, description: "not found" });
});

test("POST /api/todos deserializes the typed request body", async () => {
  const body = {
    name: "Walk dog",
    done: true,
    testInt0: 0,
    testInt1: 1,
    testInt2: 3,
    testNum0: 0.0,
    testNum1: 1.1,
    testNum2: 3.5,
  };

  const res = await fetch("http://localhost/api/todos", {
    method: "POST",
    body: JSON.stringify(body),
  });

  expect(res.status).toBe(200);
  expect(await res.json()).toEqual({ id: todo.id, ...body });
});

test("baseUrl option matches requests under an absolute origin", async () => {
  const scoped = setupServer(
    wirespec(
      GetTodos.api,
      async () => GetTodos.response200({ xTotal: 0, body: [] }),
      { baseUrl: "https://api.example.com" },
    ),
  );
  scoped.listen({ onUnhandledRequest: "error" });
  try {
    const res = await fetch("https://api.example.com/api/todos");
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual([]);
  } finally {
    scoped.close();
  }
});
