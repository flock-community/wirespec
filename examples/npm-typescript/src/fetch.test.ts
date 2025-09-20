import { HandleFetch, wirespecFetch } from "@flock/wirespec/fetch";
import { expect, test, vi } from "vitest";
import { Wirespec } from "./gen/Wirespec";

// @ts-ignore
const mockHandler = vi.fn<HandleFetch>(() =>
  Promise.resolve({
    headers: new Map([
      ["Content-Type", "application/json"],
      ["Content-Length", "2"],
    ]),
    text: () => Promise.resolve("{}"),
  })
);

test("wirespecFetch", async () => {
  const req: Wirespec.RawRequest = {
    method: "GET",
    path: ["test"],
    queries: {
      test: "test",
    },
    headers: {
      test: "TEST",
    },
    body: "",
  };
  const res = await wirespecFetch(req, mockHandler);

  expect(mockHandler).toHaveBeenCalledTimes(1);
  expect(res.body).toEqual("{}");
});
