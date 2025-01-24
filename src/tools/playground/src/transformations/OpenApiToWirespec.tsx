import { community } from "wirespec";

import Converters = community.flock.wirespec.plugin.npm.Converters;
import convert = community.flock.wirespec.plugin.npm.convert;
import emit = community.flock.wirespec.plugin.npm.emit;
import Emitters = community.flock.wirespec.plugin.npm.Emitters;
import WsEmitted = community.flock.wirespec.compiler.lib.WsEmitted;

export const openApiV2ToWirespec: (openapi: string) => WsEmitted[] = (x: string) => {
    const ast = convert(x, Converters.OPENAPI_V2);
    return emit(ast, Emitters.WIRESPEC, '');
};

export const openApiV3ToWirespec: (openapi: string) => WsEmitted[] = (x: string) => {
    const ast = convert(x, Converters.OPENAPI_V3);
    return emit(ast, Emitters.WIRESPEC, '');
};
