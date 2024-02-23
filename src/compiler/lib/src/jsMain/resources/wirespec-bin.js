#!/usr/bin/env node
const { lib } = require("./wirespec-src-compiler-lib.js");
lib(process.argv.slice(2))
