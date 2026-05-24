#!/usr/bin/env node
import { startLsp } from "./wirespec-src-plugin-npm.mjs";

const args = process.argv.slice(2);
const useNodeIpc = args.includes("--node-ipc");

startLsp(useNodeIpc);
