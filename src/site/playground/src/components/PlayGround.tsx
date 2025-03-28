import Editor from "@monaco-editor/react";
import { Language } from "../routes";

interface PlayGroundProps {
  code: string;
  setCode?: (input: string) => void;
  language: Language;
  fontSize: number;
}

export function PlayGround({ code, setCode, language, fontSize }: PlayGroundProps) {
    return (
    <Editor
      language={language}
      theme="vs-dark"
      height="100vh"
      options={{ minimap: { enabled: false }, fontSize  }}
      value={code}
      onChange={
        setCode ? (code: string | undefined) => setCode(code ?? "") : undefined
      }
    />
  );
}
