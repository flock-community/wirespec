import Editor from "@monaco-editor/react";
import { Language } from "../routes";

interface PlayGroundInputProps {
  code: string;
  setCode: (input: string) => void;
  language: Language;
}

export function PlayGroundInput({
  code,
  setCode,
  language,
}: PlayGroundInputProps) {
  return (
    <Editor
      language={language}
      theme="vs-dark"
      height="100vh"
      options={{ minimap: { enabled: false } }}
      value={code}
      onChange={(code: string | undefined) => setCode(code ?? "")}
    />
  );
}
