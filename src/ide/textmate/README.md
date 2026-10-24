# Wirespec TextMate Grammar

[TextMate grammar](https://macromates.com/manual/en/language_grammars) for Wirespec (`source.wirespec`, `.ws` files),
covering every definition: `type` (shapes, refined types, unions and default values), `enum`, `endpoint` (methods, paths,
path parameters, query parameters, headers and responses), `channel`, `rpc` and `part`, as well as annotations and comments.

It is the format GitHub ([Linguist](https://github.com/github-linguist/linguist)), VS Code, Sublime Text, Shiki and most
other highlighters understand. The [VS Code extension](../vscode) ships it, so files are colored before the language
server starts.

## Test

```shell
npm i
npm test
```

The tests tokenize with [vscode-textmate](https://github.com/microsoft/vscode-textmate), the engine VS Code uses. Next to
the targeted cases they tokenize every `.ws` file and every ` ```wirespec ` block in the docs of this repository, and fail
on any token the grammar leaves unscoped.
