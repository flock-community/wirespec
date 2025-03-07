import {
  Converters,
  convert,
  emit,
  Emitters,
  WsEmitted,
} from "@flock/wirespec";

export const openApiV2ToWirespec: (openapi: string) => WsEmitted[] = (
  x: string,
) => {
  const ast = convert(x, Converters.OPENAPI_V2);
  return emit(ast, Emitters.WIRESPEC, "");
};

export const openApiV3ToWirespec: (openapi: string) => WsEmitted[] = (
  x: string,
) => {
  const ast = convert(x, Converters.OPENAPI_V3);
  return emit(ast, Emitters.WIRESPEC, "");
};

export const avroToWirespec: (avro: string) => WsEmitted[] = (x: string) => {
  const ast = convert(x, Converters.AVRO);
  return emit(ast, Emitters.WIRESPEC, "");
};
