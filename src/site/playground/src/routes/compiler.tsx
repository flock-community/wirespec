import { createFileRoute } from "@tanstack/react-router";
import { useSearch } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { WsError, WsEmitted } from "@flock/wirespec";
import { useMonaco } from "@monaco-editor/react";
import { PlayGroundInput } from "../components/PlayGroundInput";
import { PlayGroundOutput } from "../components/PlayGroundOutput";
import { PlayGroundErrors } from "../components/PlayGroundErrors";
import { TargetLanguageSelector } from "../components/TargetLanguageSelector";
import { initializeMonaco } from "../utils/InitializeMonaco";
import { setMonacoErrors } from "../utils/SetMonacoErrors";
import { Box, Grid, Typography, useTheme } from "@mui/material";
import { wirespecToTarget } from "../transformations/WirespecToTarget";
import { wsExample } from "../examples/wirespec";

export type Emitter =
  | "typescript"
  | "kotlin"
  | "scala"
  | "java"
  | "open_api_v2"
  | "open_api_v3"
  | "avro";

type Search = {
  emitter: Emitter;
};

export type CompilationResult = {
  result: WsEmitted[];
  errors: WsError[];
};

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

export const Route = createFileRoute("/compiler")({
  component: RouteComponent,
  validateSearch: (search?: Record<string, unknown>): Search => {
    return {
      emitter: (search?.emitter as Search["emitter"]) || "typescript",
    };
  },
});

function RouteComponent() {
  const { emitter } = useSearch({ from: "/compiler" });
  const monaco = useMonaco();
  const [code, setCode] = useState(wsExample());

  useEffect(() => {
    if (!monaco) {
      return;
    }
    initializeMonaco(monaco);
  }, [monaco]);
  const [wirespecOutput, setWirespecOutput] = useState<CompilationResult>();
  const [wirespecResult, setWirespecResult] = useState("");
  const [errors, setErrors] = useState<WsError[]>([]);

  const theme = useTheme();

  useEffect(() => {
    const compiledOutput = wirespecToTarget(code, emitter);
    setWirespecOutput(compiledOutput);
  }, [code, emitter]);
  useEffect(() => {
    if (wirespecOutput) {
      if (wirespecOutput.result.length) {
        setWirespecResult(
          wirespecOutput.result
            .map(
              (file) =>
                `${createFileHeaderFor(file.typeName, emitter)}${file.result}`,
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
        <PlayGroundOutput code={wirespecResult} language={emitter} />
      </Grid>

      <Grid item xs={6}>
        <PlayGroundErrors errors={errors} />
      </Grid>
      <Grid item xs={6} />
    </Grid>
  );
}
