---
title: Npm
slug: /plugins/npm
sidebar_position: 2
---

# Wirespec NPM

![NPM Version](https://img.shields.io/npm/v/%40flock%2Fwirespec)

The Wirespec NPM plugin allows you to seamlessly integrate Wirespec into your JavaScript ecosystem. It provides a command-line interface (CLI) tool and an ES module bundle for compiling Wirespec definitions, with first-class support for TypeScript.

## Features

*   **CLI Tool:** Compile Wirespec files directly from your command line.
*   **ES Module Bundle:** Use Wirespec programmatically in your JavaScript/TypeScript projects.
*   **TypeScript Support:** Generate TypeScript definitions from your Wirespec files.

## Installation

Install the package globally using npm:

```bash
npm install -g @flock/wirespec
```

Or, install it as a dev dependency in your project:

```bash
npm install --save-dev @flock/wirespec
```

## Use

You can use the wirespec cli in your npm project. For all the cli options see [CLI](/docs/plugins/cli). 

```json
{
  "name": "project",
  "version": "0.0.0",
  "scripts": {
    "generate": "wirespec compile -i ./wirespec -o ./src/gen -l TypeScript -p ''"
  }
}
```

## Programmatically

Wirespec can be used programaticly to build your custom integrations. Here is a simple example of how to emit Typescript code.

```typescript
import fs from 'node:fs/promises';
import { parse, emit, Emitters } from 'wirespec'

const src = await fs.readFile('/todo.ws')
const ast = parse(src)
const ts = emit(ast.result, Emitters.TYPESCRIPT, '')
```
