import Editor from "@monaco-editor/react";
import { useSearch } from "@tanstack/react-router";

interface PlayGroundOutputProps {
  code?: string;
}

export function PlayGroundOutput({ code }: PlayGroundOutputProps) {
  const { output } = useSearch({ from: "/" });

  return (
    <>
      {code != undefined ? (
        <Editor
          language={output}
          theme={"vs-dark"}
          height={"50vh"}
          value={code}
          options={{ readOnly: true }}
        />
      ) : (
        ""
      )}
    </>
  );
}
