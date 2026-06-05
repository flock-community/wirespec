import { HandleFetch, wirespecFetchIr } from "@flock/wirespec/fetch";
import { expect, test, vi } from "vitest";
import { Wirespec } from "./gen/Wirespec";

// @ts-ignore
const mockHandler = vi.fn<HandleFetch>(() =>
  Promise.resolve({
    status: 200,
    headers: new Map([
      ["Content-Type", "application/json"],
      ["Content-Length", "2"],
    ]),
    arrayBuffer: () => Promise.resolve(new TextEncoder().encode("{}").buffer),
  }),
);

test("wirespecFetchIr", async () => {
  const req: Wirespec.RawRequest = {
    method: "GET",
    path: ["test"],
    queries: {
      test: ["test"],
    },
    headers: {
      test: ["TEST"],
    },
    body: undefined,
  };
  const res = await wirespecFetchIr(req, mockHandler);

  expect(mockHandler).toHaveBeenCalledTimes(1);
  expect(res.body).toEqual(new TextEncoder().encode("{}"));
});
