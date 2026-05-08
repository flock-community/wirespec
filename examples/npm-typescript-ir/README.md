# npm-typescript-ir

A minimal example showing how the `@flock/wirespec` npm plugin generates
TypeScript code from a Wirespec source file (`wirespec/meetup.ws`) via the
IR pipeline. Generation only — no fetch client, no server, no integration
tests beyond the runnable demo described below.

## Generate

```bash
npm install
npm run generate
```

Generated TypeScript lands in `src/gen/` and is gitignored.

## Build (generate + typecheck)

```bash
npm run build
```

## Generator demo

The example ships a runnable demo of the `@flock/wirespec/generator`
subpath: deterministic, regex-aware test data generation backed by the
Kotlin-native [Kotest][kotest] property-test arbs.

[kotest]: https://kotest.io

Run it:

```bash
npm run demo
```

The demo (`src/example-generator.ts`) covers three scenarios:

1. **Default usage** — `kotestWirespecGenerator(42)` returns a generator with
   the preinstalled `email` and `ipAddress` registrations. `MeetupGenerator.generate(gen, [])`
   produces a fully populated `Meetup` whose `attendees[*].email` look like real
   emails.

2. **Determinism** — calling `kotestWirespecGenerator(42)` twice with the same
   seed and generating the same type yields byte-identical output. Useful for
   reproducible test fixtures.

3. **Custom registration** — pass a `Record<string, (seed: number) => string>`
   as the second argument to override or extend the registry. The demo overrides
   `email` with `demo+${seed}@example.com`. Names are matched
   case-insensitively against `@Generator("name")` field annotations in the
   `.ws` source.

The Kotlin-side JVM extras (`uuid`, `firstName`, `lastName`, `fullName`/`name`,
`username`, `domain`, `color`) are not available in the npm distribution
because `kotest-property-arbs` doesn't ship a Kotlin/JS-IR-compatible artifact.
Register your own via the second argument when you need them.
