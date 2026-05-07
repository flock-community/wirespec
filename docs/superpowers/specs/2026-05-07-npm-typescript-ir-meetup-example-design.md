# npm-typescript-ir meetup example — design

**Date:** 2026-05-07
**Status:** Approved
**Scope:** Add a new minimal example under `examples/npm-typescript-ir/` that
demonstrates Wirespec → TypeScript code generation through the IR pipeline,
using a meetup-app spec. Code generation only — no runtime tests, no client,
no server.

## Goal

Provide a stripped-down, easy-to-read example that shows how the
`@flock/wirespec` npm plugin generates TypeScript from a Wirespec source file.
The existing `examples/npm-typescript/` example combines generation with a
non-trivial test harness (vitest, fetch tests, client/server proxies). This
new example sits alongside it as a "generation only" reference.

The npm plugin already runs all generation through the IR pipeline (its
`Main.kt` only imports `*IrEmitter` classes — `JavaIrEmitter`,
`KotlinIrEmitter`, `TypeScriptIrEmitter`, `PythonIrEmitter`). The `-ir` suffix
in the directory name is a label that distinguishes this example from the
existing one; it does not imply a separate code path.

## Non-goals

- No fetch client, no server, no integration tests.
- No prettier / formatting setup.
- No multi-language emission from the same source — TypeScript only.
- No publication to npm (`"private": true`).

## Layout

```
examples/npm-typescript-ir/
├── package.json
├── tsconfig.json
└── wirespec/
    └── meetup.ws
```

`src/gen/` is created by `npm run generate` and is not checked in.

## Wirespec source — `wirespec/meetup.ws`

```
type MeetupId = String(/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/g)
type AttendeeId = String(/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/g)

enum MeetupStatus {
    SCHEDULED, CANCELLED, COMPLETED
}

type Venue {
    name: String,
    address: String,
    capacity: Integer
}

type Attendee {
    id: AttendeeId,
    name: String,
    email: String
}

type Meetup {
    id: MeetupId,
    title: String,
    description: String,
    status: MeetupStatus,
    venue: Venue,
    attendees: Attendee[]
}

type CreateMeetup {
    title: String,
    description: String,
    venue: Venue
}

type MeetupError {
    code: Integer,
    message: String
}

endpoint GetMeetups GET /api/meetups ? {status: MeetupStatus?} -> {
    200 -> Meetup[]
}

endpoint GetMeetupById GET /api/meetups/{id: MeetupId} -> {
    200 -> Meetup
    404 -> MeetupError
}

endpoint PostMeetup POST CreateMeetup /api/meetups -> {
    201 -> Meetup
    400 -> MeetupError
}
```

This exercises the IR pipeline end-to-end:

- **`Refined`** — regex-constrained `MeetupId` / `AttendeeId`.
- **`Enum`** — `MeetupStatus`.
- **`Type`** with nested types and arrays — `Meetup` referencing `Venue` and
  `Attendee[]`.
- **`Endpoint`** — path params, optional query params, request body, multiple
  responses.

## `package.json`

```json
{
  "name": "npm-typescript-ir",
  "version": "1.0.0",
  "private": true,
  "description": "Wirespec IR-pipeline code-generation example (TypeScript, meetup app)",
  "license": "Apache-2.0",
  "scripts": {
    "generate": "wirespec compile -i ./wirespec -o ./src/gen -l TypeScript --shared",
    "build": "npm run generate && npm run typecheck",
    "typecheck": "tsc --noEmit",
    "clean": "npm run clean:generated && npm run clean:node_modules",
    "clean:generated": "npx --yes rimraf ./src/gen",
    "clean:node_modules": "npx --yes rimraf ./node_modules"
  },
  "devDependencies": {
    "@flock/wirespec": "file://../../src/plugin/npm/build/dist/js/productionLibrary",
    "typescript": "^5.6.2"
  }
}
```

Notes:

- `generate` matches the invocation used by `examples/npm-typescript/`.
- `--shared` emits the small runtime helper module (`Wirespec.ts`) that the
  generated code imports from.
- `build` runs `generate` then `typecheck` so a regression in the
  TypeScript IR emitter that produces invalid output gets caught.
- No vitest, no prettier, no fetch client, no server libs.
- `private: true` to prevent accidental publish.
- `typescript` is the only non-wirespec dev dependency.

## `tsconfig.json`

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,
    "noEmit": true,
    "skipLibCheck": true
  },
  "include": ["src/gen/**/*.ts"]
}
```

Just enough for `tsc --noEmit` over the generated output. `strict: true`
ensures we flag any IR-emitter regression that produces TypeScript that
fails strict checks.

## `examples/Makefile` integration

Append to the `build` target:

```makefile
(cd npm-typescript-ir && npm ci && npm run build) && \
```

Append to the `clean` target:

```makefile
(cd npm-typescript-ir && npm run clean)
```

No hookup to `format` (no prettier) or `yolo` (no tests to skip).

## Verification

Manual one-time verification before committing the example:

1. Build the npm plugin so the local
   `file://../../src/plugin/npm/build/dist/js/productionLibrary` is up to
   date:
   `./gradlew :src:plugin:npm:jsNodeProductionLibraryDistribution`.
2. `cd examples/npm-typescript-ir && npm install`
3. `npm run build` succeeds (generates TS, then `tsc --noEmit` passes).
4. `npm run clean` removes `src/gen` and `node_modules`.
5. `make -C examples build` still succeeds with the new entry.

## Out of scope

- Creating an "ir" CLI flag or alternate emitter selection — the npm plugin
  is already IR-only.
- Cross-language demo from the same source.
- Documentation pages outside the spec dir (no README in the example dir).
- Any change to the `examples/npm-typescript/` example.
