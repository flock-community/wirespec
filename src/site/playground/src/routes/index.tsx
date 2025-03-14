import { createFileRoute } from "@tanstack/react-router";
import { useSearch } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { WsError, WsEmitted } from "@flock/wirespec";
import { useMonaco } from "@monaco-editor/react";
import { Box, Grid, useTheme } from "@mui/material";
import { PlayGroundInput } from "../components/PlayGroundInput";
import { PlayGroundOutput } from "../components/PlayGroundOutput";
import { PlayGroundErrors } from "../components/PlayGroundErrors";
import { SpecificationSelector } from "../components/SpecificationSelector";
import { EmitterSelector } from "../components/EmitterSelector";
import { initializeMonaco } from "../utils/InitializeMonaco";
import { setMonacoErrors } from "../utils/SetMonacoErrors";
import { wirespecToTarget } from "../transformations/WirespecToTarget";
import { wsExample } from "../examples/wirespec";

export type Specification = "wirespec";

export type Emitter =
  | "typescript"
  | "kotlin"
  | "scala"
  | "java"
  | "open_api_v2"
  | "open_api_v3"
  | "avro";

type Search = {
  specification: Specification;
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

export const Route = createFileRoute("/")({
  component: RouteComponent,
  validateSearch: (search?: Record<string, unknown>): Search => {
    return {
      specification:
        (search?.specification as Search["specification"]) || "wirespec",
      emitter: (search?.emitter as Search["emitter"]) || "typescript",
    };
  },
});

function RouteComponent() {
  const monaco = useMonaco();
  const theme = useTheme();
  const { emitter } = useSearch({ from: "/" });
  const [code, setCode] = useState(wsExample());
  const [wirespecOutput, setWirespecOutput] = useState<CompilationResult>();
  const [wirespecResult, setWirespecResult] = useState("");
  const [errors, setErrors] = useState<WsError[]>([]);

  useEffect(() => {
    const compiledOutput = wirespecToTarget(code, emitter);
    setWirespecOutput(compiledOutput);
  }, [code, emitter]);

  useEffect(() => {
    if (!monaco) {
      return;
    }
    initializeMonaco(monaco);
  }, [monaco]);

  useEffect(() => {
    if (wirespecOutput) {
      if (wirespecOutput.result.length) {
        setWirespecResult(
          wirespecOutput.result
            .map(
              (file) =>
                `${createFileHeaderFor(file.typeName, emitter)}${file.result}`
            )
            .join("")
        );
      }
      setErrors(wirespecOutput.errors);
    }
  }, [wirespecOutput, emitter]);

  useEffect(() => {
    if (!monaco) {
      return;
    }
    setMonacoErrors(monaco, errors);
  }, [errors, monaco]);

  return (
    <Grid container alignItems="center" spacing={1}>
      <Grid item md={6}>
        {/* <SpecificationSelector /> */}
      </Grid>
      <Grid item md={6}>
        <Box sx={{ [theme.breakpoints.down("md")]: { display: "none" } }}>
          <EmitterSelector />
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
