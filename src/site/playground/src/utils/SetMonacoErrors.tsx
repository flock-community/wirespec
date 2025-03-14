import { Monaco } from "@monaco-editor/react";

export type MonacoError = {
  line: number;
  length: number;
  position: number;
  value: string;
};

export function setMonacoErrors(monaco: Monaco, errors: MonacoError[]) {
  const [model] = monaco.editor.getModels();

  monaco.editor.setModelMarkers(
    model,
    "owner",
    errors.map(({ line, length, position, value }) => ({
      startLineNumber: line,
      endLineNumber: line,
      startColumn: position - length,
      endColumn: position + length,
      message: value,
      severity: monaco.MarkerSeverity.Error,
    })),
  );
}
