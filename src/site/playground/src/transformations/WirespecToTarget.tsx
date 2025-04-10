import { emit, Emitters, parse, WsEmitted, WsError } from "@flock/wirespec";
import { CompilationResult, Emitter, Language } from "../routes";

const getEmitterFor: (emitter: Emitter) => Emitters = (emitter: Emitter) => {
  switch (emitter) {
    case "wirespec":
      return Emitters.WIRESPEC;
    case "typescript":
      return Emitters.TYPESCRIPT;
    case "kotlin":
      return Emitters.KOTLIN;
    case "scala":
      return Emitters.SCALA;
    case "python":
      return Emitters.PYTHON;
    case "java":
      return Emitters.JAVA;
    case "open_api_v2":
      return Emitters.OPENAPI_V2;
    case "open_api_v3":
      return Emitters.OPENAPI_V3;
    case "avro":
      return Emitters.AVRO;
  }
};

const getLanguageFor: (emitter: Emitter) => Language = (emitter: Emitter) => {
  switch (emitter) {
    case "typescript":
    case "kotlin":
    case "scala":
    case "python":
    case "java":
    case "wirespec":
      return emitter;
    case "open_api_v2":
    case "open_api_v3":
    case "avro":
      return "json";
  }
};

export const wirespecToTarget: (
  wirespec: string,
  emitter: Emitter,
) => CompilationResult = (wirespec, emitter) => {
  const internalEmitter = getEmitterFor(emitter);
  const parseResult = parse(wirespec);
  let result = [] as WsEmitted[];
  let errors = [] as WsError[];
  if (parseResult.result) {
    result = emit(
      parseResult.result,
      internalEmitter,
      "community.flock.wirespec.generated",
    );
    if (
      internalEmitter === Emitters.OPENAPI_V3 ||
      internalEmitter === Emitters.OPENAPI_V2 ||
      internalEmitter === Emitters.AVRO
    ) {
      result = result.map((it) => ({
        typeName: it.typeName,
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
