import Editor from "@monaco-editor/react";

interface PlayGroundInputProps {
  code: string;
  setCode: (input: string) => void;
}

export function PlayGroundInput({ code, setCode }: PlayGroundInputProps) {
  return (
    <Editor
      language="wirespec"
      height={"80vh"}
      theme="vs-dark"
      options={{ minimap: { enabled: false } }}
      value={code}
      onChange={(code: string | undefined) => setCode(code ?? "")}
    />
  );
}
