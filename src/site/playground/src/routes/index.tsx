import { createFileRoute } from "@tanstack/react-router";
import { useSearch } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { WsError, WsEmitted } from "@flock/wirespec";
import { useMonaco } from "@monaco-editor/react";
import { Box, styled } from "@mui/material";
import { PlayGroundInput } from "../components/PlayGroundInput";
import { PlayGroundOutput } from "../components/PlayGroundOutput";
import { SpecificationSelector } from "../components/SpecificationSelector";
import { EmitterSelector } from "../components/EmitterSelector";
import { initializeMonaco } from "../utils/InitializeMonaco";
import { MonacoError, setMonacoErrors } from "../utils/SetMonacoErrors";
import { wirespecToTarget } from "../transformations/WirespecToTarget";
import { wsExample } from "../examples/wirespec";
import { swaggerExample } from "../examples/swagger";
import {
  openApiV2ToWirespec,
  openApiV3ToWirespec,
} from "../transformations/OpenAPIToWirespec";

type CompileSpecification = "wirespec";
type ConvertSpecification = "open_api_v2" | "open_api_v3";

export type Specification = CompileSpecification | ConvertSpecification;

export type CompilerEmitter =
  | "typescript"
  | "kotlin"
  | "scala"
  | "java"
  | "open_api_v2"
  | "open_api_v3"
  | "avro";
export type ConverterEmitter = "wirespec";
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
    case "wirespec":
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
  height: "100%",
  display: "flex",
  gap: "8px",
  [theme.breakpoints.down("md")]: {
    flexDirection: "column",
  },
}));

function RouteComponent() {
  const monaco = useMonaco();
  const { emitter, specification } = useSearch({ from: "/" });
  const [code, setCode] = useState("");
  const [wirespecOutput, setWirespecOutput] = useState<CompilationResult>();
  const [wirespecResult, setWirespecResult] = useState("");
  const [wirespecErrors, setWirespecErrors] = useState<MonacoError[]>([]);

  useEffect(() => {
    if (specification === "wirespec") {
      const compiledOutput = wirespecToTarget(code, emitter);
      setWirespecOutput(compiledOutput);
      return;
    }

    try {
      const json = JSON.parse(code);

      if (json.swagger) {
        setWirespecOutput(openApiV2ToWirespec(code));
      } else if (json.openapi) {
        setWirespecOutput(openApiV3ToWirespec(code));
      } else {
        return setWirespecErrors([
          {
            value: `Invalid OpenAPI specification; missing 'swagger' or 'openapi' property`,
            line: 1,
            position: 1,
            length: 1,
          },
        ]);
      }
      setWirespecErrors([]);
    } catch (error) {
      if (error instanceof Error) {
        setWirespecErrors([
          { value: error.message, line: 1, position: 1, length: 1 },
        ]);
      }
    }
  }, [code, emitter, specification]);

  useEffect(() => {
    if (!monaco) return;
    initializeMonaco(monaco);
  }, [monaco]);

  useEffect(() => {
    if (!monaco) return;

    setMonacoErrors(monaco, wirespecErrors);
  }, [wirespecErrors, monaco]);

  useEffect(() => {
    specification === "wirespec" ? setCode(wsExample) : setCode(swaggerExample);
  }, [specification]);

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
      if (wirespecOutput.errors) {
        setWirespecErrors(wirespecOutput.errors);
      }
    }
  }, [wirespecOutput, emitter]);

  return (
    <StyledContainer>
      <Box flex={1}>
        <SpecificationSelector />
        <Box marginTop={1} height="100%">
          <PlayGroundInput code={code} setCode={setCode} />
        </Box>
      </Box>
      <Box flex={1}>
        <EmitterSelector />
        <Box marginTop={1} minHeight="80vh" height="100%">
          <PlayGroundOutput code={wirespecResult} language={emitter} />
        </Box>
      </Box>
    </StyledContainer>
  );
}
