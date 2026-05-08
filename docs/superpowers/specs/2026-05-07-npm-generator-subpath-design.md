# `@flock/wirespec/generator` npm subpath — design

**Date:** 2026-05-07
**Status:** Approved
**Scope:** Expose the existing `:src:integration:kotest` Wirespec.Generator
implementation to TypeScript projects through a new subpath export of the
existing `@flock/wirespec` npm package.

## Goal

TypeScript users of `@flock/wirespec` who emit `XxxGenerator.ts` files via the
`--ir` codegen path can today *call* the generator factories
(`MeetupGenerator.generate(gen, [])`) but must hand-write a
`Wirespec.Generator` implementation themselves. The Kotlin side ships
`kotestWirespecGenerator(...)` (in `:src:integration:kotest`) — a deterministic,
seed-driven, regex-aware generator with a name registry and `@Seed` semantics —
and the module is already multiplatform (JVM + JS/IR).

This spec adds a new `@flock/wirespec/generator` subpath export so the same
implementation is available to TypeScript users straight from the npm package
they already install.

## Non-goals

- No native-TypeScript port. The npm distribution bundles the existing
  Kotlin/JS implementation.
- No new gradle module: the kotest module is folded into the existing
  `:src:plugin:npm` Kotlin/JS bundle.
- No second npm package. One artifact (`@flock/wirespec`), one new subpath.
- No equivalent of the JVM-only `kotest-property-arbs` extras (`uuid`,
  `firstName`, `lastName`, `fullName`/`name`, `username`, `domain`, `color`).
  Those remain JVM-only because the upstream artifact has no Kotlin/JS-IR
  build.
- No runtime test framework on the TS side (no vitest/jest). The example
  in `examples/npm-typescript-ir/` runs a node script that prints output;
  end-to-end verification is "the script runs and prints sensible data."

## Architecture

```
src/integration/kotest (commonMain + jsMain)
        │
        │  jsMain adds @JsExport facade with TS-friendly registry shape
        │
        ▼
:src:plugin:npm (gains kotest as an implementation dep)
        │
        │  Kotlin/JS emits  wirespec-src-integration-kotest.mjs
        │                   plus kotest-property + rgxgen runtime klibs
        │
        ▼
src/plugin/npm/src/jsMain/resources/
  wirespec-generator.mjs   thin shim: re-exports the @JsExport facade
  wirespec-generator.d.ts  hand-written clean TS types

build.gradle.kts adds  "./generator" -> wirespec-generator.{mjs,d.ts}
        │
        ▼
TypeScript user:
  import { kotestWirespecGenerator } from '@flock/wirespec/generator'
  const gen = kotestWirespecGenerator(1, { orderId: (s) => `ORD-${s}` })
  const meetup = MeetupGenerator.generate(gen, [])
```

The pattern matches the existing `./fetch` and `./serialization` subpaths,
which also live as hand-written `.mjs` + `.d.ts` files in
`src/plugin/npm/src/jsMain/resources/`.

The new bundle weight inside `@flock/wirespec` is the Kotlin/JS klib of
`io.kotest:kotest-property` core. `kotlinx-rgxgen` is already present (used by
the wirespec compiler for regex-validated `String(/.../)` types).

## Component changes

### 1. `src/integration/kotest/build.gradle.kts`

Promote `kotest.property` and `:src:integration:wirespec` from `compileOnly` to
`implementation` in `commonMain`. The existing `compileOnly` was correct for
JVM consumers (where the test suite supplies the dep). For the npm bundle the
runtime klib must reach the Kotlin/JS output of `:src:plugin:npm`.

### 2. `src/plugin/npm/build.gradle.kts`

Add to the `jsMain` source-set dependencies:

```kotlin
implementation(project(":src:integration:kotest"))
implementation(project(":src:integration:wirespec"))
```

Add to the `customField("exports", …)` map:

```kotlin
"./generator" to mapOf(
    "types"   to "./wirespec-generator.d.ts",
    "default" to "./wirespec-generator.mjs",
),
```

### 3. New Kotlin file: `KotestWirespecGeneratorJs.kt`

Path: `src/integration/kotest/src/jsMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJs.kt`

```kotlin
@file:OptIn(ExperimentalJsExport::class)
@file:JsExport

package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map

/**
 * TS-callable entry point. The DSL/Arb-based [kotestWirespecGenerator] cannot
 * be `@JsExport`ed (lambda receivers + generic `Arb<T>` don't survive
 * Kotlin/JS export), so this thin facade adapts a plain
 * `Record<string, (seed: number) => string>` registry to the same underlying
 * `KotestWirespecGenerator` algorithm.
 *
 * From TypeScript:
 * ```ts
 * const gen = kotestWirespecGeneratorJs(1, { orderId: (s) => `ORD-${s}` })
 * ```
 */
fun kotestWirespecGeneratorJs(
    seed: Int,
    registrations: dynamic = null,
): Wirespec.Generator = kotestWirespecGenerator(seed.toLong()) {
    if (registrations != null) {
        val keys = js("Object").keys(registrations) as Array<String>
        for (key in keys) {
            val factory = registrations[key].unsafeCast<(Int) -> String>()
            register(key) {
                Arb.long().map { factory(it.toInt()) }
            }
        }
    }
}
```

Design choices:

- **Seed type `Int`**: round-trips as a JS `number`, capped at 2^31. Fine for
  arbitrary discriminator use; cheaper than the `Long` opaque-class export.
- **Registrations as `dynamic`**: gives TS the cleanest call-site
  (`{ orderId: (s) => …`}`) and the unsafe cast is one line. Typed `Map`
  exports as `ReadonlyMap` and forces TS users into `new Map(Object.entries(…))`
  boilerplate.
- **No changes to `commonMain`**: the JVM/Kotlin DSL surface
  (`kotestWirespecGenerator(seed, block)`) is untouched.

### 4. `wirespec-generator.d.ts`

Path: `src/plugin/npm/src/jsMain/resources/wirespec-generator.d.ts`

```ts
/**
 * Wirespec.Generator backed by Kotest Arbs (Kotlin/JS), with a
 * deterministic seed and an optional name registry for `@Generator("name")`
 * fields.
 *
 * Default catalog (preinstalled): `email`, `ipAddress`. JVM-only extras
 * (`uuid`, `firstName`, `lastName`, `fullName`/`name`, `username`, `domain`,
 * `color`) are not available in the npm distribution because
 * `kotest-property-arbs` doesn't ship a Kotlin/JS-IR-compatible artifact.
 * Register custom names via the second argument.
 */

export type Type = string;

export interface GeneratorField<T extends any | undefined> {}

export type GeneratorFieldString  = { kind: "string",  regex: string | undefined, annotations: Record<string, any>[] };
export type GeneratorFieldInteger = { kind: "integer", min: number | undefined, max: number | undefined, annotations: Record<string, any>[] };
export type GeneratorFieldNumber  = { kind: "number",  min: number | undefined, max: number | undefined, annotations: Record<string, any>[] };
export type GeneratorFieldBoolean = { kind: "boolean", annotations: Record<string, any>[] };
export type GeneratorFieldBytes   = { kind: "bytes",   annotations: Record<string, any>[] };
export type GeneratorFieldEnum    = { kind: "enum",    values: string[], annotations: Record<string, any>[], type: Type };
export type GeneratorFieldUnion   = { kind: "union",   variants: string[], annotations: Record<string, any>[], type: Type };
export type GeneratorFieldArray<T>    = { kind: "array",    generate: (p0: string[]) => T };
export type GeneratorFieldNullable<T> = { kind: "nullable", generate: (p0: string[]) => T };
export type GeneratorFieldShape<T>    = { kind: "shape",    annotations: Record<string, Record<string, any>[]>, generate: (p0: string[]) => T, type: Type };
export type GeneratorFieldDict<V>     = { kind: "dict",     generate: (p0: string[]) => V };

export interface WirespecGenerator {
  generate<T>(path: string[], field: GeneratorField<T>): T;
}

export type GeneratorRegistrations = Record<string, (seed: number) => string>;

/**
 * @param seed   Deterministic seed (0..2^31-1). Same seed + same generated
 *               type → identical output.
 * @param registrations  Optional `@Generator("name")` registry. Names are
 *               matched case-insensitively. Overrides defaults.
 */
export declare function kotestWirespecGenerator(
  seed?: number,
  registrations?: GeneratorRegistrations,
): WirespecGenerator;
```

The `GeneratorField*` definitions are re-declared here (a deliberate copy of
the codegen output) so TS gets full discriminated-union safety on calls into
`generator.generate(...)`. See the *Drift guard* section under Verification.

The exported `WirespecGenerator` is structurally compatible with the
codegen-emitted `Wirespec.Generator` interface; users assign without casting.

### 5. `wirespec-generator.mjs`

Path: `src/plugin/npm/src/jsMain/resources/wirespec-generator.mjs`

```js
// Re-exports the @JsExport facade from the bundled Kotlin/JS module under
// the TS-friendly name. The Kotlin facade is `kotestWirespecGeneratorJs`
// (with the `Js` suffix) so it doesn't collide with the Kotlin DSL function
// of the same root name in commonMain.
import { kotestWirespecGeneratorJs } from './wirespec-src-integration-kotest.mjs';

export function kotestWirespecGenerator(seed = 0, registrations) {
    return kotestWirespecGeneratorJs(seed, registrations);
}
```

The Kotlin/JS bundle filename is gradle-derived from the project path:
`:src:integration:kotest` → `wirespec-src-integration-kotest.mjs`. This
matches the existing dist's naming for sibling modules
(`wirespec-src-compiler-core.mjs`, `wirespec-src-converter-avro.mjs`, etc.).

### 6. `examples/npm-typescript-ir/` — generator demo

The existing meetup example is extended with a runnable demonstration that
also serves as the structural-compat smoke test. New file:
`src/example-generator.ts` (full source in *Verification* below). New
`package.json` script `npm run demo` runs it via `tsx`; the existing
`tsconfig.json` is extended to cover `src/**/*.ts` so `npm run typecheck`
includes the demo file alongside the codegen output.

The example's stated scope (per its existing design doc) widens slightly: it
remains "no fetch client, no server, no integration tests," but now does
include a node-runnable demo that prints generator output. The README gains
a short "Generator demo" section.

## Verification

### Kotlin tests

`./gradlew :src:integration:kotest:allTests` runs the existing JVM and JS
`commonTest` suite. `commonMain` is unchanged, so behavior is preserved on
both targets. A new `KotestWirespecGeneratorJsTest` in `jsTest` covers:

- `kotestWirespecGeneratorJs(1)` returns a working `Wirespec.Generator` against
  a hand-built `GeneratorFieldString`.
- `kotestWirespecGeneratorJs(1, registrations)` with a `dynamic` plain-object
  registry routes `@Generator("orderId")` lookups through the user's function.
- Same seed → same output across two invocations.
- Default catalog still applies: `@Generator("email")` produces an
  email-shaped string.

### npm-bundle integrity

`./gradlew :src:plugin:npm:assemble` produces
`build/dist/js/productionLibrary/` containing both `wirespec-generator.mjs`
and `wirespec-generator.d.ts`, plus `wirespec-src-integration-kotest.mjs` and
the kotest-property runtime klib.

### TypeScript demo + smoke test

`examples/npm-typescript-ir/` is extended with a runnable demo file that
doubles as the structural-compat smoke test for the new subpath. The example
already wires `@flock/wirespec` from the local `productionLibrary` build via
`file://`, so it picks up the new subpath without an npm publish.

**New file:** `examples/npm-typescript-ir/src/example-generator.ts`

```ts
import { kotestWirespecGenerator } from '@flock/wirespec/generator';
import { MeetupGenerator } from './gen/generator/MeetupGenerator';
import { AttendeeGenerator } from './gen/generator/AttendeeGenerator';

// 1. Default usage — preinstalled `email` and `ipAddress` arbs apply.
const gen = kotestWirespecGenerator(42);
const meetup = MeetupGenerator.generate(gen, []);
console.log('Meetup (seed=42):\n', JSON.stringify(meetup, null, 2));

// 2. Determinism — same seed produces identical output.
const replay = MeetupGenerator.generate(kotestWirespecGenerator(42), []);
console.log(
    'Deterministic replay matches:',
    JSON.stringify(meetup) === JSON.stringify(replay),
);

// 3. Custom registration — override @Generator("email") with a domain-specific
//    factory. Names are case-insensitive.
const customGen = kotestWirespecGenerator(7, {
    email: (s) => `demo+${s}@example.com`,
});
const attendee = AttendeeGenerator.generate(customGen, []);
console.log('Attendee with custom email factory:\n', JSON.stringify(attendee, null, 2));
```

The file is both a *demo* (run via `npm run demo`) and a *smoke test*: if the
shim's `WirespecGenerator` type isn't structurally compatible with the
codegen-emitted `Wirespec.Generator`, the assignment passed into
`MeetupGenerator.generate(gen, [])` won't typecheck and `npm run typecheck`
fails.

**`package.json` changes:**

```json
{
    "scripts": {
        "generate": "wirespec compile -i ./wirespec -o ./src/gen -l TypeScript --shared --ir",
        "build":    "npm run generate && npm run typecheck",
        "typecheck": "tsc --noEmit",
        "demo":     "tsx src/example-generator.ts",
        "clean":    "npm run clean:generated && npm run clean:node_modules",
        "clean:generated":    "npx --yes rimraf ./src/gen",
        "clean:node_modules": "npx --yes rimraf ./node_modules"
    },
    "devDependencies": {
        "@flock/wirespec": "file://../../src/plugin/npm/build/dist/js/productionLibrary",
        "tsx": "^4.19.0",
        "typescript": "^5.6.2"
    }
}
```

The demo runs through [`tsx`][tsx], a TypeScript-aware Node loader. It runs
the `.ts` source directly with no emit step, sidestepping Node ESM's
explicit-`.js`-extension requirement (the codegen output uses
extensionless imports). One devDep, no `dist/` to manage. The plain
`tsconfig.json` covers `src/**/*.ts` for `npm run typecheck`.

[tsx]: https://github.com/privatenumber/tsx

**README addition:**

A short section under a new "Generator demo" heading explains:

- `npm run demo` runs the three blocks and prints generated `Meetup` and
  `Attendee` records.
- What each block illustrates (default catalog, determinism, custom
  registry).
- That `@Generator("name")` annotations in the `.ws` source route to the
  registry; the meetup spec uses default-name fields, so the demo also
  shows a custom override of `email`.

### Drift guard for the re-declared union

The `GeneratorField*` types in `wirespec-generator.d.ts` are a copy of the
TypeScript codegen output. To catch drift, the existing TypeScript IR emitter
test asserts a snapshot of these specific type definitions: any change to the
codegen for `Wirespec.GeneratorField*` triggers a snapshot mismatch, and the
fix is to update both the codegen output and `wirespec-generator.d.ts` in
lockstep.

## Acceptance criteria

- `./gradlew :src:integration:kotest:allTests :src:plugin:npm:jsTest` passes.
- `./gradlew :src:plugin:npm:assemble` produces a `productionLibrary/`
  containing `wirespec-generator.{mjs,d.ts}`.
- `cd examples/npm-typescript-ir && npm install && npm run build` succeeds
  (runs `generate && typecheck` over `src/example-generator.ts`).
- `npm run demo` in the same example prints a fully-populated `Meetup`
  (including email-shaped `attendees[*].email`), confirms deterministic
  replay, and prints an `Attendee` whose `email` matches the custom
  `demo+<seed>@example.com` pattern. Manual run, not in CI.

## Out-of-scope future work

- TS-native extras to backfill the JVM-only catalog (`uuid` via
  `crypto.randomUUID`, name lists, etc.). Achievable as a follow-up if user
  demand emerges.
- A separate `@flock/wirespec-kotest` npm package, splitting the testing
  runtime out of the codegen CLI. Considered and rejected (option A2 in the
  brainstorm); revisit only if `@flock/wirespec` bundle size becomes an issue.
- Native fast-check integration. Out of scope for this design.
