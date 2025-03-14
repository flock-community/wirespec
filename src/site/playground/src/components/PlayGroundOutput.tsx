import Editor from "@monaco-editor/react";

interface PlayGroundOutputProps {
  code: string;
  language: string;
}

export function PlayGroundOutput({ code, language }: PlayGroundOutputProps) {
  return (
    <Editor
      language={language}
      height={"75vh"}
      theme="vs-dark"
      options={{ readOnly: true, minimap: { enabled: false } }}
      value={code}
    />
  );
}
