import { parse, emit, Emitters, WsEmitted, WsError } from "@flock/wirespec";
import { CompilationResult } from "../routes/index";

const getEmitterFor = (language: string) => {
  switch (language) {
    case "typescript":
      return Emitters.TYPESCRIPT;
    case "kotlin":
      return Emitters.KOTLIN;
    case "scala":
      return Emitters.SCALA;
    case "java":
      return Emitters.JAVA;
    case "open_api_v2":
      return Emitters.OPENAPI_V2;
    case "open_api_v3":
      return Emitters.OPENAPI_V3;
    case "avro":
      return Emitters.AVRO;
    default:
      throw new Error(`unknown language: ${language}`);
  }
};

export const wirespecToTarget: (x: string, y: string) => CompilationResult = (
  wirespec: string,
  language: string,
) => {
  const emitter = getEmitterFor(language);
  const parseResult = parse(wirespec);
  let result = [] as WsEmitted[];
  let errors = [] as WsError[];
  if (parseResult.result) {
    result = emit(parseResult.result, emitter, "");
  }
  if (parseResult.errors) {
    errors = parseResult.errors;
  }
  return { result, errors };
};
