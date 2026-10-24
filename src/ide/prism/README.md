# Wirespec Prism Grammar

[Prism](https://prismjs.com) language definition for Wirespec, registered as `wirespec` and `ws`. It powers the code blocks
of the [docs site](../../site/docs).

## Usage

The module exports a function that registers the language on a Prism instance:

```js
const Prism = require("prismjs");
require("@flock/wirespec-prism")(Prism);

Prism.highlight(source, Prism.languages.wirespec, "wirespec");
```

Loaded with a `<script>` tag after Prism, it registers itself on the global `Prism`.

## Test

```shell
npm i
npm test
```

Next to the targeted cases, the tests tokenize every `.ws` file and every ` ```wirespec ` block in the docs of this
repository, and fail on any text the grammar leaves untokenized.
