import { OnMount } from "@monaco-editor/react";

export type CodeEditor = Parameters<OnMount>[0];

// Matches the declaration of an endpoint's raw conversion functions in every
// emitted language, but not their call sites: a declaration line ends with
// `{` (Java, Scala, TypeScript, Kotlin), `=` (Kotlin, Scala) or `:` (Python).
const RAW_CONVERSION_DECLARATION =
  /\b(toRawRequest|fromRawRequest|toRawResponse|fromRawResponse)\(.*[{=:]\s*$/;

export function foldRawConversions(editor: CodeEditor) {
  const selectionLines = (editor.getModel()?.getLinesContent() ?? []).flatMap(
    (line, index) => (RAW_CONVERSION_DECLARATION.test(line) ? [index] : []),
  );

  if (selectionLines.length === 0) return;

  // Folding reveals the cursor, which sits at the end of the file after the
  // editor replaces its content, so restore the scroll position afterwards.
  const scrollTop = editor.getScrollTop();
  editor
    .getAction("editor.fold")
    ?.run({ levels: 1, direction: "down", selectionLines })
    .then(() => editor.setScrollTop(scrollTop));
}
