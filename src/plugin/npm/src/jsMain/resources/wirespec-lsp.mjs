#!/usr/bin/env node
import { startLsp } from "./wirespec-src-plugin-npm.mjs";

// The server reads the sibling files of a document from disk through kotlinx-io, which looks
// for a global `require`: https://github.com/Kotlin/kotlinx-io/issues/345
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

const args = process.argv.slice(2);
const useNodeIpc = args.includes("--node-ipc");

startLsp(useNodeIpc);
