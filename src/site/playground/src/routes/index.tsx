import { createFileRoute } from "@tanstack/react-router";
import { useSearch } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { WsError, WsEmitted } from "@flock/wirespec";
import { useMonaco } from "@monaco-editor/react";
import { Box, styled } from "@mui/material";
import { PlayGroundInput } from "../components/PlayGroundInput";
import { PlayGroundOutput } from "../components/PlayGroundOutput";
import { PlayGroundErrors } from "../components/PlayGroundErrors";
import { SpecificationSelector } from "../components/SpecificationSelector";
import { EmitterSelector } from "../components/EmitterSelector";
import { initializeMonaco } from "../utils/InitializeMonaco";
import { setMonacoErrors } from "../utils/SetMonacoErrors";
import { wirespecToTarget } from "../transformations/WirespecToTarget";
import { wsExample } from "../examples/wirespec";

type CompileSpecification = "wirespec";
type ConvertSpecification = "open_api_v2" | "open_api_v3";

export type Specification = CompileSpecification | ConvertSpecification;

type CompilerEmitter =
  | "typescript"
  | "kotlin"
  | "scala"
  | "java"
  | "open_api_v2"
  | "open_api_v3"
  | "avro";
type ConverterEmitter = "wirespec";

export type Emitter = CompilerEmitter | ConverterEmitter;

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

const StyledContainer = styled(Box)(({ theme }) => ({
  display: "flex",
  gap: "8px",
  [theme.breakpoints.down("md")]: {
    flexDirection: "column",
  },
}));

function RouteComponent() {
  const monaco = useMonaco();
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
    <StyledContainer>
      <Box flex={1}>
        <SpecificationSelector />
        <Box marginTop={1}>
          <PlayGroundInput code={code} setCode={setCode} />
        </Box>
        <PlayGroundErrors errors={errors} />
      </Box>
      <Box flex={1}>
        <EmitterSelector />
        <Box marginTop={1}>
          <PlayGroundOutput code={wirespecResult} language={emitter} />
        </Box>
      </Box>
    </StyledContainer>
  );
}
