#!/usr/bin/env node
const cli = require("./wirespec-src-plugin-cli.js");
cli.community.flock.wirespec.plugin.cli.cli(process.argv.slice(2))
