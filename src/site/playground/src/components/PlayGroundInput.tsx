import Editor from "@monaco-editor/react";

interface PlayGroundInputProps {
  code: string;
  setCode: (input: string) => void;
}

export function PlayGroundInput({ code, setCode }: PlayGroundInputProps) {
  return (
    <Editor
      language="wirespec"
      theme="vs-dark"
      height="100vh"
      options={{ minimap: { enabled: false } }}
      value={code}
      onChange={(code: string | undefined) => setCode(code ?? "")}
    />
  );
}
