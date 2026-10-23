import {
  emit,
  Emitters,
  Extensions,
  parse,
  WsEmitted,
  WsError,
} from "@flock/wirespec";
import { CompilationResult, Emitter, Extension, Language } from "../routes";

const getEmitterFor: (emitter: Emitter) => Emitters = (emitter: Emitter) => {
  switch (emitter) {
    case "wirespec":
      return Emitters.WIRESPEC;
    case "typescript":
      return Emitters.TYPESCRIPT;
    case "kotlin":
      return Emitters.KOTLIN;
    case "python":
      return Emitters.PYTHON;
    case "java":
      return Emitters.JAVA;
    case "scala":
      return Emitters.SCALA;
    case "open_api_v2":
      return Emitters.OPENAPI_V2;
    case "open_api_v3":
      return Emitters.OPENAPI_V3;
    case "avro":
      return Emitters.AVRO;
    case "avro_idl":
      return Emitters.AVRO_IDL;
    case "graphql":
      return Emitters.GRAPHQL;
    case "asyncapi":
      return Emitters.ASYNCAPI;
  }
};

const extensions: Record<Extension, Extensions> = {
  avro: Extensions.AVRO,
  builder: Extensions.BUILDER,
  jackson: Extensions.JACKSON,
  kotest: Extensions.KOTEST,
  kotlinx_serialization: Extensions.KOTLINX_SERIALIZATION,
  spring: Extensions.SPRING,
  spring_native_hints: Extensions.SPRING_NATIVE_HINTS,
};

export const isExtension = (value: unknown): value is Extension =>
  Object.keys(extensions).includes(value as string);

export const supports = (extension: Extension, emitter: Emitter) =>
  extensions[extension].supports(getEmitterFor(emitter));

const getLanguageFor: (emitter: Emitter) => Language = (emitter: Emitter) => {
  switch (emitter) {
    case "typescript":
    case "kotlin":
    case "python":
    case "java":
    case "scala":
    case "wirespec":
    case "graphql":
      return emitter;
    case "open_api_v2":
    case "open_api_v3":
    case "avro":
    case "asyncapi":
      return "json";
    case "avro_idl":
      return "avdl";
  }
};

export const PACKAGE_NAME = "community.flock.wirespec.generated";

export const wirespecToTarget: (
  wirespec: string,
  emitter: Emitter,
  selected: Extension[],
) => CompilationResult = (wirespec, emitter, selected) => {
  const internalEmitter = getEmitterFor(emitter);
  const parseResult = parse(wirespec);
  let result = [] as WsEmitted[];
  let errors = [] as WsError[];
  if (parseResult.result) {
    result = emit(
      parseResult.result,
      internalEmitter,
      PACKAGE_NAME,
      false,
      selected.map((extension) => extensions[extension]),
    );
    if (
      internalEmitter === Emitters.OPENAPI_V3 ||
      internalEmitter === Emitters.OPENAPI_V2 ||
      internalEmitter === Emitters.AVRO ||
      internalEmitter === Emitters.ASYNCAPI
    ) {
      result = result.map((it) => ({
        file: it.file,
        result: prettyJson(it.result),
      }));
    }
  }
  if (parseResult.errors) {
    errors = parseResult.errors;
  }
  return { result, errors, language: getLanguageFor(emitter) };
};

const prettyJson = (code: string) => {
  return JSON.stringify(JSON.parse(code), null, 2);
};
