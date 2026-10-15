import Editor from "@monaco-editor/react";
import { useEffect, useState } from "react";
import { Language } from "../routes";
import { Box } from "@mui/material";
import { CodeEditor } from "../utils/FoldRawConversions";

interface PlayGroundProps {
  code: string;
  setCode?: (input: string) => void;
  language: Language;
  // Called once the editor shows `code`: after mounting and after every change.
  onCodeShown?: (editor: CodeEditor) => void;
  // Fill the parent instead of taking half the viewport.
  fill?: boolean;
}

export function PlayGround({
  code,
  setCode,
  language,
  onCodeShown,
  fill,
}: PlayGroundProps) {
  const [editor, setEditor] = useState<CodeEditor>();

  // The editor updates its content in its own effects, which run before this
  // one, so the editor already holds `code` here.
  useEffect(() => {
    if (editor) onCodeShown?.(editor);
  }, [editor, code, onCodeShown]);

  return (
    <Box
      sx={{
        width: fill
          ? "100%"
          : {
              xs: "100vw",
              sm: "50vw",
            },
      }}
    >
      <Editor
        language={language}
        theme="vs-dark"
        height="100vh"
        options={{ minimap: { enabled: false }, fontSize: 14 }}
        value={code}
        onMount={setEditor}
        onChange={
          setCode
            ? (code: string | undefined) => setCode(code ?? "")
            : undefined
        }
      />
    </Box>
  );
}
