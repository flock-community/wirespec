# Example: How to use Wirespec with TypeScript

## Node Npm TypeScript Configuration

According to the [actual package.json](package.json) file.

## Mocking with MSW

`@flock/wirespec/msw` turns a generated endpoint into a typed
[Mock Service Worker](https://mswjs.io) request handler:

```ts
import { setupServer } from "msw/node";
import { wirespec } from "@flock/wirespec/msw";
import { GetTodoById } from "./gen/endpoint";

const server = setupServer(
  wirespec(GetTodoById.api, async (req) =>
    GetTodoById.response200({ body: { id: req.path.id, /* ... */ } }),
  ),
);
```

The resolver receives the deserialized Wirespec request and must return one of
that endpoint's responses — returning another endpoint's response is a compile
error. Pass `{ baseUrl }` to pin a host or path prefix. See
[`src/msw.ts`](src/msw.ts) for a runnable example and
[`src/msw.test.ts`](src/msw.test.ts) for the behavioral tests.

> `msw` is an optional peer dependency of `@flock/wirespec`; install it in your
> project to use this integration.
