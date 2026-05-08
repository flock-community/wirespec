# `@flock/wirespec/generator` npm subpath — implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose the existing `:src:integration:kotest` `Wirespec.Generator` implementation to TypeScript users as `@flock/wirespec/generator`, and demonstrate it with a runnable demo in `examples/npm-typescript-ir/`.

**Architecture:** Fold the kotest module's Kotlin/JS bundle into `:src:plugin:npm` and add a thin `@JsExport` facade (`kotestWirespecGeneratorJs`) that adapts a plain JS-object registry to the existing DSL. Hand-written `wirespec-generator.{mjs,d.ts}` shim files in `src/plugin/npm/src/jsMain/resources/` re-export the facade under TS-friendly names — same precedent as the existing `./fetch` and `./serialization` subpaths.

**Tech Stack:** Kotlin Multiplatform (JVM + JS/IR), Kotest property-test DSL, Gradle, npm, TypeScript, Node.

**Spec:** `docs/superpowers/specs/2026-05-07-npm-generator-subpath-design.md`

---

## File Structure

**Modify:**
- `src/integration/kotest/build.gradle.kts` — promote two `compileOnly` deps to `implementation`
- `src/plugin/npm/build.gradle.kts` — add kotest + wirespec-runtime deps; add `./generator` to `exports` map
- `examples/npm-typescript-ir/tsconfig.json` — extend `include` to cover `src/**/*.ts`
- `examples/npm-typescript-ir/package.json` — add `demo` script + `tsx` devDep
- `examples/npm-typescript-ir/README.md` — add Generator demo section

**Create:**
- `src/integration/kotest/src/jsMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJs.kt` — `@JsExport` facade
- `src/integration/kotest/src/jsTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJsTest.kt` — facade tests
- `src/plugin/npm/src/jsMain/resources/wirespec-generator.d.ts` — TS types
- `src/plugin/npm/src/jsMain/resources/wirespec-generator.mjs` — TS shim
- `examples/npm-typescript-ir/src/example-generator.ts` — runnable demo

**Note on the runner:** the original brainstorm settled on `tsc + node`, but Node ESM requires explicit `.js` extensions in import specifiers and the codegen emits extensionless imports. Switching the demo runner to `tsx` (a single devDep) avoids that mismatch without polluting the demo source with `await import(...)` wrappers or relying on the removed `--experimental-specifier-resolution=node` flag.

---

## Task 1: Smoke test for `kotestWirespecGeneratorJs` (TDD red)

**Files:**
- Create: `src/integration/kotest/src/jsTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJsTest.kt`

- [ ] **Step 1: Write the failing smoke test**

```kotlin
package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KotestWirespecGeneratorJsTest {

    @Test
    fun `kotestWirespecGeneratorJs returns a working generator with no registrations`() {
        val gen = kotestWirespecGeneratorJs(seed = 1)
        val field = Wirespec.GeneratorFieldString(regex = null, annotations = emptyList())
        @Suppress("UNCHECKED_CAST")
        val value = gen.generate(listOf("a"), field) as String
        assertNotNull(value)
        assertTrue(value.isNotEmpty(), "expected non-empty string, got '$value'")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :src:integration:kotest:jsTest --tests "*KotestWirespecGeneratorJsTest.kotestWirespecGeneratorJs returns a working generator with no registrations"`

Expected: FAIL with compilation error `Unresolved reference 'kotestWirespecGeneratorJs'` — the function doesn't exist yet.

- [ ] **Step 3: Implement minimal facade**

Create `src/integration/kotest/src/jsMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJs.kt`:

```kotlin
@file:OptIn(ExperimentalJsExport::class)
@file:JsExport

package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec

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
): Wirespec.Generator = kotestWirespecGenerator(seed.toLong())
```

The `registrations` parameter is accepted but ignored at this stage — Task 2 wires it in.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :src:integration:kotest:jsTest --tests "*KotestWirespecGeneratorJsTest.kotestWirespecGeneratorJs returns a working generator with no registrations"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/integration/kotest/src/jsMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJs.kt \
        src/integration/kotest/src/jsTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJsTest.kt
git commit -m "feat(integration/kotest): add @JsExport facade for TS consumers"
```

---

## Task 2: Registry support in the facade (TDD red)

**Files:**
- Modify: `src/integration/kotest/src/jsTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJsTest.kt`
- Modify: `src/integration/kotest/src/jsMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJs.kt`

- [ ] **Step 1: Write the failing registry test**

Append to `KotestWirespecGeneratorJsTest`:

```kotlin
    @Test
    fun `dynamic registrations object routes named generators through user functions`() {
        val regs: dynamic = js("({orderId: function(s) { return 'ORD-' + s; }})")
        val gen = kotestWirespecGeneratorJs(seed = 1, registrations = regs)
        val field = Wirespec.GeneratorFieldString(
            regex = null,
            annotations = listOf(
                mapOf(
                    "name" to "Generator",
                    "parameters" to mapOf("default" to "orderId"),
                ),
            ),
        )
        @Suppress("UNCHECKED_CAST")
        val value = gen.generate(listOf("a"), field) as String
        assertTrue(value.startsWith("ORD-"), "expected 'ORD-...', got '$value'")
    }

    @Test
    fun `registry name match is case-insensitive`() {
        val regs: dynamic = js("({Email: function(s) { return 'CASE-' + s + '@x'; }})")
        val gen = kotestWirespecGeneratorJs(seed = 1, registrations = regs)
        val field = Wirespec.GeneratorFieldString(
            regex = null,
            annotations = listOf(
                mapOf(
                    "name" to "Generator",
                    "parameters" to mapOf("default" to "email"),
                ),
            ),
        )
        @Suppress("UNCHECKED_CAST")
        val value = gen.generate(listOf("b"), field) as String
        assertTrue(value.startsWith("CASE-"), "expected case-insensitive override, got '$value'")
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :src:integration:kotest:jsTest --tests "*KotestWirespecGeneratorJsTest*"`

Expected: 2 FAIL — both new tests fail, the smoke test still passes. The two new failures are because `registrations` is currently ignored, so `@Generator("orderId")` falls through to "Unknown @Generator name" error.

- [ ] **Step 3: Extend the facade**

Replace the body of `kotestWirespecGeneratorJs` in `KotestWirespecGeneratorJs.kt` so the file reads:

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

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :src:integration:kotest:jsTest --tests "*KotestWirespecGeneratorJsTest*"`

Expected: 3 PASS (smoke + 2 registry tests).

- [ ] **Step 5: Commit**

```bash
git add src/integration/kotest/src/jsMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJs.kt \
        src/integration/kotest/src/jsTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJsTest.kt
git commit -m "feat(integration/kotest): adapt dynamic JS registrations to Arb registry"
```

---

## Task 3: Determinism regression test

**Files:**
- Modify: `src/integration/kotest/src/jsTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJsTest.kt`

- [ ] **Step 1: Write the determinism test**

Append to `KotestWirespecGeneratorJsTest`:

```kotlin
    @Test
    fun `same seed produces identical output across two invocations`() {
        val field = Wirespec.GeneratorFieldString(regex = null, annotations = emptyList())
        @Suppress("UNCHECKED_CAST")
        val a = kotestWirespecGeneratorJs(seed = 42).generate(listOf("a"), field) as String
        @Suppress("UNCHECKED_CAST")
        val b = kotestWirespecGeneratorJs(seed = 42).generate(listOf("a"), field) as String
        assertEquals(a, b)
    }
```

Add at the top of the file alongside the other `kotlin.test` imports:

```kotlin
import kotlin.test.assertEquals
```

- [ ] **Step 2: Run test to verify it passes**

Run: `./gradlew :src:integration:kotest:jsTest --tests "*KotestWirespecGeneratorJsTest.same seed produces identical output across two invocations"`

Expected: PASS — the underlying `KotestWirespecGenerator` algorithm in `commonMain` is deterministic per seed; this test guards against future regressions in the facade.

- [ ] **Step 3: Commit**

```bash
git add src/integration/kotest/src/jsTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJsTest.kt
git commit -m "test(integration/kotest): add determinism regression test for JS facade"
```

---

## Task 4: Promote `compileOnly` deps in the kotest module

**Files:**
- Modify: `src/integration/kotest/build.gradle.kts`

**Why:** When `:src:plugin:npm` consumes `:src:integration:kotest` (Task 5), the runtime klibs of `kotest-property` and `:src:integration:wirespec` need to flow into the npm bundle. `compileOnly` doesn't transit; `implementation` does.

- [ ] **Step 1: Edit the `commonMain` source-set deps**

In `src/integration/kotest/build.gradle.kts`, replace this block:

```kotlin
        commonMain {
            dependencies {
                compileOnly(project(":src:integration:wirespec"))
                compileOnly(libs.kotest.property)
                implementation(libs.kotlinx.rgxgen)
            }
        }
```

with:

```kotlin
        commonMain {
            dependencies {
                implementation(project(":src:integration:wirespec"))
                implementation(libs.kotest.property)
                implementation(libs.kotlinx.rgxgen)
            }
        }
```

Leave `jvmMain`'s `compileOnly(libs.kotest.property.arbs)` unchanged — JVM extras stay JVM-only.

- [ ] **Step 2: Run the full kotest module test suite to verify nothing breaks**

Run: `./gradlew :src:integration:kotest:allTests`

Expected: PASS. Promoting from `compileOnly` to `implementation` is safe — the source code didn't change, only how the dep is exposed to consumers.

- [ ] **Step 3: Commit**

```bash
git add src/integration/kotest/build.gradle.kts
git commit -m "chore(integration/kotest): promote kotest-property and wirespec runtime to implementation"
```

---

## Task 5: Wire kotest module into the npm plugin

**Files:**
- Modify: `src/plugin/npm/build.gradle.kts`

- [ ] **Step 1: Add the dependency lines**

In `src/plugin/npm/build.gradle.kts`, find the `jsMain` source-set block:

```kotlin
        jsMain {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:compiler:lib"))
                implementation(project(":src:plugin:arguments"))
                implementation(project(":src:plugin:cli"))
                implementation(project(":src:converter:openapi"))
                implementation(project(":src:converter:avro"))
                implementation(project(":src:tools:generator"))
                implementation(libs.kotlinx.openapi.bindings)
                implementation(libs.kotlinx.serialization)
            }
        }
```

Add two lines so the block becomes:

```kotlin
        jsMain {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:compiler:lib"))
                implementation(project(":src:plugin:arguments"))
                implementation(project(":src:plugin:cli"))
                implementation(project(":src:converter:openapi"))
                implementation(project(":src:converter:avro"))
                implementation(project(":src:tools:generator"))
                implementation(project(":src:integration:wirespec"))
                implementation(project(":src:integration:kotest"))
                implementation(libs.kotlinx.openapi.bindings)
                implementation(libs.kotlinx.serialization)
            }
        }
```

- [ ] **Step 2: Add `./generator` to the exports map**

In the same file, find the `customField("exports", …)` block and add the new entry alongside `./fetch` and `./serialization`:

```kotlin
            customField(
                "exports",
                mapOf(
                    "." to mapOf(
                        "types" to "./wirespec-src-plugin-npm.d.ts",
                        "default" to "./wirespec-src-plugin-npm.mjs",
                    ),
                    "./fetch" to mapOf(
                        "types" to "./wirespec-fetch.d.ts",
                        "default" to "./wirespec-fetch.mjs",
                    ),
                    "./serialization" to mapOf(
                        "types" to "./wirespec-serialization.d.ts",
                        "default" to "./wirespec-serialization.mjs",
                    ),
                    "./generator" to mapOf(
                        "types" to "./wirespec-generator.d.ts",
                        "default" to "./wirespec-generator.mjs",
                    ),
                ),
            )
```

- [ ] **Step 3: Build the productionLibrary**

Run: `./gradlew :src:plugin:npm:assemble`

Expected: PASS. Build completes; the kotest module's Kotlin/JS klib gets bundled.

- [ ] **Step 4: Verify the kotest module's bundle ended up in the dist**

Run: `ls src/plugin/npm/build/dist/js/productionLibrary/wirespec-src-integration-kotest.mjs`

Expected: file exists. (`.d.ts` and `.mjs.map` should also be present.)

- [ ] **Step 5: Verify the package.json `exports` includes `./generator`**

Run: `grep -A 3 '"./generator"' src/plugin/npm/build/dist/js/productionLibrary/package.json`

Expected: output shows the `./generator` export with `types` and `default` paths. The shim files don't exist yet (those come in Tasks 6–7), but the `exports` map is correctly populated.

- [ ] **Step 6: Commit**

```bash
git add src/plugin/npm/build.gradle.kts
git commit -m "feat(plugin/npm): bundle kotest integration and add ./generator export"
```

---

## Task 6: Add `wirespec-generator.d.ts` shim

**Files:**
- Create: `src/plugin/npm/src/jsMain/resources/wirespec-generator.d.ts`

- [ ] **Step 1: Create the file with full type declarations**

Create `src/plugin/npm/src/jsMain/resources/wirespec-generator.d.ts`:

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

- [ ] **Step 2: Rebuild and verify it ends up in the dist**

Run: `./gradlew :src:plugin:npm:assemble`

Then: `ls src/plugin/npm/build/dist/js/productionLibrary/wirespec-generator.d.ts`

Expected: file exists in the dist. Kotlin/JS auto-copies `jsMain/resources` into the production library.

- [ ] **Step 3: Commit**

```bash
git add src/plugin/npm/src/jsMain/resources/wirespec-generator.d.ts
git commit -m "feat(plugin/npm): add wirespec-generator.d.ts type shim"
```

---

## Task 7: Add `wirespec-generator.mjs` shim

**Files:**
- Create: `src/plugin/npm/src/jsMain/resources/wirespec-generator.mjs`

- [ ] **Step 1: Create the runtime shim**

Create `src/plugin/npm/src/jsMain/resources/wirespec-generator.mjs`:

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

- [ ] **Step 2: Rebuild and verify it ends up in the dist**

Run: `./gradlew :src:plugin:npm:assemble`

Then: `ls src/plugin/npm/build/dist/js/productionLibrary/wirespec-generator.mjs`

Expected: file exists in the dist alongside `wirespec-src-integration-kotest.mjs`.

- [ ] **Step 3: Smoke-check the import resolves**

Run:

```bash
cd src/plugin/npm/build/dist/js/productionLibrary && \
  node --input-type=module -e "import('./wirespec-generator.mjs').then(m => console.log(typeof m.kotestWirespecGenerator))"
```

Expected output: `function`. This proves the shim's import of `wirespec-src-integration-kotest.mjs` resolves and the named export exists.

- [ ] **Step 4: Commit**

```bash
git add src/plugin/npm/src/jsMain/resources/wirespec-generator.mjs
git commit -m "feat(plugin/npm): add wirespec-generator.mjs runtime shim"
```

---

## Task 8: Broaden `tsconfig.json` to cover the demo source

**Files:**
- Modify: `examples/npm-typescript-ir/tsconfig.json`

**Why:** The current `tsconfig.json` includes only `src/gen/**/*.ts`. The new demo file lives at `src/example-generator.ts` and must be type-checked alongside the codegen output. We're not adding a second build config — `tsx` runs `.ts` directly without an emit step.

- [ ] **Step 1: Replace `tsconfig.json` to broaden `include`**

Replace `examples/npm-typescript-ir/tsconfig.json` content:

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
  "include": ["src/**/*.ts"]
}
```

Only the `include` glob changed (`src/gen/**/*.ts` → `src/**/*.ts`).

- [ ] **Step 2: Commit**

```bash
git add examples/npm-typescript-ir/tsconfig.json
git commit -m "chore(example/npm-typescript-ir): broaden tsconfig include to src/**/*.ts"
```

---

## Task 9: Update `package.json` scripts and add `tsx` devDep

**Files:**
- Modify: `examples/npm-typescript-ir/package.json`

- [ ] **Step 1: Update `package.json`**

Replace `examples/npm-typescript-ir/package.json` content:

```json
{
  "name": "npm-typescript-ir",
  "version": "1.0.0",
  "private": true,
  "description": "Wirespec IR-pipeline code-generation example (TypeScript, meetup app)",
  "license": "Apache-2.0",
  "scripts": {
    "generate": "wirespec compile -i ./wirespec -o ./src/gen -l TypeScript --shared --ir",
    "build": "npm run generate && npm run typecheck",
    "typecheck": "tsc --noEmit",
    "demo": "tsx src/example-generator.ts",
    "clean": "npm run clean:generated && npm run clean:node_modules",
    "clean:generated": "npx --yes rimraf ./src/gen",
    "clean:node_modules": "npx --yes rimraf ./node_modules"
  },
  "devDependencies": {
    "@flock/wirespec": "file://../../src/plugin/npm/build/dist/js/productionLibrary",
    "tsx": "^4.19.0",
    "typescript": "^5.6.2"
  }
}
```

Diff vs. before: `demo` script is new; `tsx` devDep is new. `typecheck` is unchanged in form. `clean` is unchanged (no `dist` to clean since `tsx` doesn't emit).

- [ ] **Step 2: Commit**

```bash
git add examples/npm-typescript-ir/package.json
git commit -m "chore(example/npm-typescript-ir): add demo script + tsx devDep"
```

---

## Task 10: Add `example-generator.ts` demo

**Files:**
- Create: `examples/npm-typescript-ir/src/example-generator.ts`

- [ ] **Step 1: Create the demo file**

Create `examples/npm-typescript-ir/src/example-generator.ts`:

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

- [ ] **Step 2: Commit**

```bash
git add examples/npm-typescript-ir/src/example-generator.ts
git commit -m "feat(example/npm-typescript-ir): add example-generator.ts demo"
```

---

## Task 11: Verify the example builds, typechecks, and runs

**Files:** none (verification-only).

**Why:** The example consumes `@flock/wirespec` via a `file://` link to the gradle-built productionLibrary. After the gradle changes in Tasks 5–7, the local `node_modules` cache must be refreshed before npm sees the new `./generator` subpath.

- [ ] **Step 1: Refresh the example's node_modules**

Run:

```bash
cd examples/npm-typescript-ir && \
  npm run clean:node_modules && \
  npm install
```

Expected: `npm install` completes; `node_modules/@flock/wirespec/wirespec-generator.{mjs,d.ts}` exist.

Verify: `ls node_modules/@flock/wirespec/wirespec-generator.{mjs,d.ts}`

- [ ] **Step 2: Regenerate codegen and typecheck**

Run (from `examples/npm-typescript-ir/`):

```bash
npm run build
```

Expected: `wirespec compile` regenerates `src/gen/`, then `tsc --noEmit` exits 0. The `example-generator.ts` import of `@flock/wirespec/generator` resolves; `MeetupGenerator.generate(gen, [])` typechecks (proves `WirespecGenerator` is structurally compatible with the codegen-emitted `Wirespec.Generator` — this is the structural-compat smoke test).

- [ ] **Step 3: Run the demo**

Run (from `examples/npm-typescript-ir/`):

```bash
npm run demo
```

Expected output (key indicators):
- A JSON `Meetup` object printed for seed 42, with `id`, `title`, `description`, a `venue`, and an `attendees` array.
- `attendees[*].email` looks like a real email (matches `*@*.*`) — proof the default `email` arb fired.
- `Deterministic replay matches: true`.
- A second JSON `Attendee` object whose `email` matches `demo+<digits>@example.com` — proof the custom registration overrode the default.

If any of these fail, do not commit; debug from the printed output.

- [ ] **Step 4: Return to the repo root**

Run: `cd ../..`

(No commit — Tasks 8–10 already committed the source changes that this task verifies.)

---

## Task 12: Document the demo in the example README

**Files:**
- Modify: `examples/npm-typescript-ir/README.md`

- [ ] **Step 1: Append a "Generator demo" section**

Append to `examples/npm-typescript-ir/README.md` (preserve existing content; add at the end):

```markdown
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
```

- [ ] **Step 2: Commit**

```bash
git add examples/npm-typescript-ir/README.md
git commit -m "docs(example/npm-typescript-ir): document generator demo"
```

---

## Final acceptance check

After Task 12 is done, run the full happy path once more from a clean state:

```bash
./gradlew :src:integration:kotest:allTests :src:plugin:npm:assemble && \
cd examples/npm-typescript-ir && \
  npm run clean && npm install && npm run build && npm run demo
```

Expected:
- Gradle reports BUILD SUCCESSFUL.
- `npm run demo` prints the three demo blocks; emails are well-formed; deterministic replay reports `true`; custom-registration attendee email matches `demo+\d+@example.com`.
