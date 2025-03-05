#!/usr/bin/env node
import {cli} from "./wirespec-src-plugin-cli.mjs";

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

cli(process.argv.slice(2))
