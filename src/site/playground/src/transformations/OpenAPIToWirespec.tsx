import { Converters, convert, emit, Emitters } from "@flock/wirespec";
import { CompilationResult } from "../routes";

export const openApiV2ToWirespec: (openapi: string) => CompilationResult = (
  x: string,
) => {
  const ast = convert(x, Converters.OPENAPI_V2);
  return {
    result: emit(ast, Emitters.WIRESPEC, ""),
    errors: [],
    language: "wirespec",
  };
};

export const openApiV3ToWirespec: (openapi: string) => CompilationResult = (
  x: string,
) => {
  const ast = convert(x, Converters.OPENAPI_V3);
  return {
    result: emit(ast, Emitters.WIRESPEC, ""),
    errors: [],
    language: "wirespec",
  };
};

export const avroToWirespec: (avro: string) => CompilationResult = (
  x: string,
) => {
  const ast = convert(x, Converters.AVRO);
  return {
    result: emit(ast, Emitters.WIRESPEC, ""),
    errors: [],
    language: "wirespec",
  };
};
