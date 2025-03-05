import Editor from "@monaco-editor/react";

interface PlayGroundInputProps {
  code: string;
  setCode: (input: string) => void;
}

export function PlayGroundInput({ code, setCode }: PlayGroundInputProps) {
  function handleSetCode(code: string | undefined) {
    if (!code) {
      setCode("");
      return;
    }
    setCode(code);
  }

  return (
    <Editor
      language="wirespec"
      theme="ws-dark"
      height={"75vh"}
      value={code}
      onChange={(code: string | undefined) => handleSetCode(code)}
    />
  );
}
