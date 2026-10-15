# Project Name

This is the documentation source of wirespec, on [wirespec.io](https://wirespec.io) (COMING SOON)

The website is built using [Docusaurus](https://docusaurus.io/), a modern static website generator.

## Maintaining / updating the documentation

As the `docs` are part of the wirespec monorepo, they will automatically be build when building the whole project ([see root readme](../../../README.md)).

To run docusaurus locally:

1. Build the Wirespec npm library, which the docs use to generate code examples. From the root of the repository:

   ```bash
   ./gradlew :src:plugin:npm:jsNodeProductionLibraryDistribution
   ```

2. Navigate to the docs folder

   ```bash
   cd src/site/docs
   ```

3. Install dependencies:
   ```bash
   npm install
   ```
4. Run docusaurus
   ```bash
   npm start
   ```

## Generated code examples

Generated code is never copied into the documentation. A page names a Wirespec file and a language in its front
matter, and every code block marked with `wirespec` is filled with the output of the Wirespec CLI when the site is
built, for example:

````md
---
wirespec:
  source: todo.ws
  language: Kotlin
  package: community.flock.todo
---

```kotlin wirespec file=model/Todo.kt
```
````

The build fails when the file does not compile or a block asks for output that is not generated. See
[`src/remark/wirespec.ts`](src/remark/wirespec.ts) for all options. The emitter pages all use
[`docs/emitters/todo.ws`](docs/emitters/todo.ws).
