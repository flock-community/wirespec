import { useEffect, useState } from "react";
import { WsError, WsEmitted } from "@flock/wirespec";
import { useMonaco } from "@monaco-editor/react";
import { PlayGroundInput } from "./PlayGroundInput";
import { PlayGroundOutput } from "./PlayGroundOutput";
import { PlayGroundErrors } from "./PlayGroundErrors";
import { TargetLanguageSelector } from "./TargetLanguageSelector";
import { initializeMonaco } from "../utils/InitializeMonaco";
import { setMonacoErrors } from "../utils/SetMonacoErrors";
import { Box, Grid, Typography, useTheme } from "@mui/material";
import { wirespecToTarget } from "../transformations/WirespecToTarget";
import { useSearch } from "@tanstack/react-router";

interface PlayGroundProps {
  code: string;
  setCode: (input: string) => void;
  selectedLanguage: string;
  setSelectedLanguage: (language: string) => void;
}

export interface CompliationResult {
  result: WsEmitted[];
  errors: WsError[];
}

function createFileHeaderFor(fileName: string, language: string) {
  switch (language) {
    case "typescript":
    case "kotlin":
    case "scala":
    case "open_api_v2":
    case "open_api_v3":
    case "avro":
      return "";
    case "java":
      return `\n/**\n/* ${fileName}\n**/\n`;
    default:
      throw `unknown language: ${language}`;
  }
}

export function PlayGround({ code, setCode }: PlayGroundProps) {
  const { output } = useSearch({ from: "/" });
  const monaco = useMonaco();
  useEffect(() => {
    if (!monaco) {
      return;
    }
    initializeMonaco(monaco);
  }, [monaco]);
  const [wirespecOutput, setWirespecOutput] = useState<CompliationResult>();
  const [wirespecResult, setWirespecResult] = useState("");
  const [errors, setErrors] = useState<WsError[]>([]);

  const theme = useTheme();

  useEffect(() => {
    const compiledOutput = wirespecToTarget(code, output);
    setWirespecOutput(compiledOutput);
  }, [code, output]);
  useEffect(() => {
    if (wirespecOutput) {
      if (wirespecOutput.result.length) {
        setWirespecResult(
          wirespecOutput.result
            .map(
              (file) =>
                `${createFileHeaderFor(file.typeName, output)}${file.result}`,
            )
            .join(""),
        );
      }
      setErrors(wirespecOutput.errors);
    }
  }, [wirespecOutput]);
  useEffect(() => {
    if (!monaco) {
      return;
    }
    setMonacoErrors(monaco, errors);
  }, [errors, monaco]);

  return (
    <>
      <Grid container alignItems="center" spacing={1}>
        <Grid item md={6}>
          <Box display="flex" alignItems="center" gap="5px">
            <Typography variant="h5">
              Wirespec
              <Box
                component="span"
                sx={{ [theme.breakpoints.up("md")]: { display: "none" } }}
              >
                {" "}
                to:
              </Box>
            </Typography>
            <Box sx={{ [theme.breakpoints.up("md")]: { display: "none" } }}>
              <TargetLanguageSelector />
            </Box>
          </Box>
        </Grid>
        <Grid item md={6}>
          <Box sx={{ [theme.breakpoints.down("md")]: { display: "none" } }}>
            <TargetLanguageSelector />
          </Box>
        </Grid>

        <Grid item xs={12} md={6}>
          <PlayGroundInput code={code} setCode={setCode} />
        </Grid>

        <Grid item xs={12} md={6}>
          <PlayGroundOutput code={wirespecResult} />
        </Grid>

        <Grid item xs={6}>
          <PlayGroundErrors errors={errors} />
        </Grid>
        <Grid item xs={6} />
      </Grid>
    </>
  );
}
