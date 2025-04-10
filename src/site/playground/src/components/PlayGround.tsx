import Editor from "@monaco-editor/react";
import { Language } from "../routes";
import { Box } from "@mui/material";

interface PlayGroundProps {
  code: string;
  setCode?: (input: string) => void;
  language: Language;
}

export function PlayGround({ code, setCode, language }: PlayGroundProps) {
  return (
    <Box
      sx={{
        width: {
          xs: "100vw",
          sm: "50vw",
        },
      }}
    >
      <Editor
        language={language}
        theme="vs-dark"
        height="100vh"
        options={{ minimap: { enabled: false } }}
        value={code}
        onChange={
          setCode
            ? (code: string | undefined) => setCode(code ?? "")
            : undefined
        }
      />
    </Box>
  );
}
