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
    GetTodoById.response200({ body: { id: req.path.id /* ... */ } }),
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

## Generating test data

`@flock/wirespec/generator` provides a seeded random data generator (backed by
the Kotest integration) that drives the generated `*Generator` namespaces to
produce values conforming to your Wirespec definitions, including refined-type
constraints such as regexes and number bounds:

```ts
import { kotestWirespecGenerator } from "@flock/wirespec/generator";
import { TodoDtoGenerator } from "./gen/generator/TodoDtoGenerator";

const generator = kotestWirespecGenerator(42);
const todo = TodoDtoGenerator.generate(generator, []);
// todo.id matches the TodoId UUID regex, todo.testInt2 is within its bounds, ...
```

The same seed always produces the same data, making generated values safe to
use in deterministic tests. See [`src/generator.test.ts`](src/generator.test.ts)
for a runnable example.
