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
 *
 * Integer-typed fields without explicit min/max bounds are drawn from the
 * full Kotlin Long range and converted to JS numbers, so values can exceed
 * `Number.MAX_SAFE_INTEGER` (2^53 - 1) and lose precision. For unconstrained
 * Integer fields, prefer narrower bounds in your `.ws` source (e.g.,
 * `Integer(0, 1000000)`) when precise round-tripping matters.
 */

export type Type = string;

export interface GeneratorField<T extends any | undefined> {}

export type GeneratorFieldString    = { kind: "string",    regex: string | undefined, annotations: Record<string, any>[] };
export type GeneratorFieldInteger64 = { kind: "integer64", min: number | undefined, max: number | undefined, annotations: Record<string, any>[] };
export type GeneratorFieldInteger32 = { kind: "integer32", min: number | undefined, max: number | undefined, annotations: Record<string, any>[] };
export type GeneratorFieldNumber64  = { kind: "number64",  min: number | undefined, max: number | undefined, annotations: Record<string, any>[] };
export type GeneratorFieldNumber32  = { kind: "number32",  min: number | undefined, max: number | undefined, annotations: Record<string, any>[] };
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
