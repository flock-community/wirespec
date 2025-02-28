import Editor from "@monaco-editor/react";

interface PlayGroundOutputProps {
  code?: string;
  language: string;
}

export function PlayGroundOutput({ code, language }: PlayGroundOutputProps) {
  return (
    <>
      {code != undefined ? (
        <Editor
          language={language}
          theme={"vs-dark"}
          height={"75vh"}
          value={code}
          options={{ readOnly: true }}
        />
      ) : (
        ""
      )}
    </>
  );
}
