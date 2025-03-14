import {
  Converters,
  convert,
  emit,
  Emitters,
  type WsEmitted,
} from "@flock/wirespec";

export const openApiV2ToWirespec: (openapi: string) => {
  result: WsEmitted[];
  errors: [];
} = (x: string) => {
  const ast = convert(x, Converters.OPENAPI_V2);
  return { result: emit(ast, Emitters.WIRESPEC, ""), errors: [] };
};

export const openApiV3ToWirespec: (openapi: string) => {
  result: WsEmitted[];
  errors: [];
} = (x: string) => {
  const ast = convert(x, Converters.OPENAPI_V3);
  return { result: emit(ast, Emitters.WIRESPEC, ""), errors: [] };
};

export const avroToWirespec: (avro: string) => {
  result: WsEmitted[];
  errors: [];
} = (x: string) => {
  const ast = convert(x, Converters.AVRO);
  return { result: emit(ast, Emitters.WIRESPEC, ""), errors: [] };
};
