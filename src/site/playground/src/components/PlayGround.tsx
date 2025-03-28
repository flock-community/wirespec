import Editor from "@monaco-editor/react";
import { Language } from "../routes";
import {useEffect, useState} from "react";

interface PlayGroundProps {
  code: string;
  setCode?: (input: string) => void;
  language: Language;
}

export function PlayGround({ code, setCode, language }: PlayGroundProps) {
    const [width, setWidth] = useState(0);
    useEffect(() => {
        const updateWindowDimensions = () => {
            const newWidth = window.innerWidth;
            setWidth(newWidth);
        };

        window.addEventListener("resize", updateWindowDimensions);

        return () => window.removeEventListener("resize", updateWindowDimensions)
    }, []);
    return (
    <Editor
      language={language}
      theme="vs-dark"
      height="100vh"
      options={{ minimap: { enabled: false }, fontSize: width < 768 ? 12 : 0.01*width }}
      value={code}
      onChange={
        setCode ? (code: string | undefined) => setCode(code ?? "") : undefined
      }
    />
  );
}
