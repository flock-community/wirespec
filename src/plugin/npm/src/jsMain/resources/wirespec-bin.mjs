#!/usr/bin/env node
import {cli} from "./wirespec-src-plugin-npm.mjs";

// https://github.com/Kotlin/kotlinx-io/issues/345
import os from "node:os";
import fs from "node:fs";
import path from "node:path";
import buffer from "node:buffer";
global.require = function require(input) {
    switch (input) {
        case "os": return os
        case "fs": return fs
        case "path": return path
        case "buffer": return buffer
    }
}

// Workaround: Clikt's ES modules have a circular dependency (clikt-core imports
// selfAndAncestors from clikt-mordant, while clikt-mordant imports CoreCliktCommand
// from clikt-core). This causes CliktCommand's prototype to chain to a stale
// CoreCliktCommand prototype instead of the properly initialized one.
import * as cliktCore from "./clikt-clikt.mjs";
import * as cliktMordant from "./clikt-clikt-mordant.mjs";
const CoreCC = Object.values(cliktCore).find(v => typeof v === 'function' && v.name === 'CoreCliktCommand');
const CC = Object.values(cliktMordant).find(v => typeof v === 'function' && v.name === 'CliktCommand');
if (CoreCC && CC && Object.getPrototypeOf(CC.prototype) !== CoreCC.prototype) {
    Object.setPrototypeOf(CC.prototype, CoreCC.prototype);
}

cli(process.argv.slice(2))
