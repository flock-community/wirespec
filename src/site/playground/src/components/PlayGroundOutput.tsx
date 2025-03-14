import Editor from "@monaco-editor/react";

interface PlayGroundOutputProps {
  code: string;
  language: string;
}

export function PlayGroundOutput({ code, language }: PlayGroundOutputProps) {
  return (
    <Editor
      language={language}
      theme="vs-dark"
      height="100vh"
      options={{ readOnly: true, minimap: { enabled: false } }}
      value={code}
    />
  );
}
