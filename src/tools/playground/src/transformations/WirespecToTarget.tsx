import { community } from "wirespec";
import parse = community.flock.wirespec.plugin.npm.parse;
import emit = community.flock.wirespec.plugin.npm.emit;
import Emitters = community.flock.wirespec.plugin.npm.Emitters;
import WsEmitted = community.flock.wirespec.compiler.lib.WsEmitted;
import WsError = community.flock.wirespec.compiler.lib.WsError;
import { CompliationResult } from "../components/PlayGround";

const getEmitterFor = (language: string) => {
  switch(language) {
    case "typescript":
      return Emitters.TYPESCRIPT;
    case "kotlin":
      return Emitters.KOTLIN
    case "scala":
      return Emitters.SCALA;
    case "java":
      return Emitters.JAVA;
    default:
      throw(`unknown language: ${language}`);
  }
}

export const wirespecToTarget: (x: string, y: string) => CompliationResult = (wirespec: string, language: string) => {
  const emitter = getEmitterFor(language);
  const parseResult = parse(wirespec);
  let result = [] as WsEmitted[];
  let errors = [] as WsError[];
  if(parseResult.result) {
      result = emit(parseResult.result, emitter, '');
  }
  if (parseResult.errors) {
      errors = parseResult.errors;
  }
  return { result, errors };
};
