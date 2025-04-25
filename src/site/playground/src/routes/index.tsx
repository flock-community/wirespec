import { createFileRoute } from "@tanstack/react-router";
import { useSearch } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { WsError, WsEmitted } from "@flock/wirespec";
import { useMonaco } from "@monaco-editor/react";
import { Box, Button } from "@mui/material";
import { PlayGround } from "../components/PlayGround";
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
import { openapiExample } from "../examples/openapi";

type CompileSpecification = "wirespec";
type ConvertSpecification = "open_api_v2" | "open_api_v3";

export type Specification = CompileSpecification | ConvertSpecification;

export type CompilerEmitter =
  | "typescript"
  | "kotlin"
  | "python"
  | "java"
  | "open_api_v2"
  | "open_api_v3"
  | "avro";
export type ConverterEmitter = "wirespec";
export type Emitter = CompilerEmitter | ConverterEmitter;

export type Language =
  | "wirespec"
  | "kotlin"
  | "java"
  | "typescript"
  | "python"
  | "json";

type Search = {
  specification: Specification;
  emitter: Emitter;
};

export type CompilationResult = {
  result: WsEmitted[];
  errors: WsError[];
  language: Language;
};

const createFileHeaderFor = (fileName: string, emitter: Emitter): string => {
  switch (emitter) {
    case "typescript":
    case "kotlin":
    case "open_api_v2":
    case "open_api_v3":
    case "avro":
    case "wirespec":
      return "";
    case "java":
    case "python":
      return `\n/**\n/* ${fileName}\n**/\n`;
  }
};

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
  const { emitter, specification } = useSearch({ from: "/" });
  const [code, setCode] = useState("");
  const [wirespecOutput, setWirespecOutput] = useState<CompilationResult>();
  const [wirespecResult, setWirespecResult] = useState("");
  const [wirespecErrors, setWirespecErrors] = useState<MonacoError[]>([]);
  const [mobileDisplay, setMobileDisplay] = useState<"input" | "output">(
    "input",
  );

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
            message: "Invalid JSON",
            startLineNumber: 1,
            startColumn: 1,
            endLineNumber: 1,
            endColumn: 1,
          },
        ]);
      }
    } catch (e) {
      return setWirespecErrors([
        {
          message: "Invalid JSON",
          startLineNumber: 1,
          startColumn: 1,
          endLineNumber: 1,
          endColumn: 1,
        },
      ]);
    }
  }, [code, emitter, specification]);

  useEffect(() => {
    if (monaco) {
      initializeMonaco(monaco);
    }
  }, [monaco]);

  useEffect(() => {
    if (monaco && wirespecOutput?.errors) {
      setMonacoErrors(monaco, wirespecOutput.errors);
    }
  }, [monaco, wirespecOutput?.errors]);

  useEffect(() => {
    if (wirespecOutput?.result) {
      const result = wirespecOutput.result
        .map((it) => {
          return `${createFileHeaderFor(it.typeName, emitter)}${it.result}`;
        })
        .join("\n\n");
      setWirespecResult(result);
    }
  }, [wirespecOutput?.result, emitter]);

  useEffect(() => {
    if (wirespecErrors) {
      setWirespecResult("");
    }
  }, [wirespecErrors]);

  useEffect(() => {
    if (specification === "wirespec") {
      setCode(wsExample);
    } else if (specification === "open_api_v2") {
      setCode(swaggerExample);
    } else if (specification === "open_api_v3") {
      setCode(openapiExample);
    }
  }, [specification]);

  return (
    <Box
      sx={{
        display: "flex",
        flexDirection: "column",
        height: "100vh",
        width: "100vw",
      }}
    >
      <Box
        sx={{
          display: "flex",
          flexDirection: "row",
          alignItems: "center",
          justifyContent: "center",
          padding: "1rem",
          gap: "1rem",
          borderBottom: "1px solid #e0e0e0",
        }}
      >
        <SpecificationSelector />
        <EmitterSelector />
        <Box
          sx={{
            display: { xs: "flex", md: "none" },
            flexDirection: "row",
            gap: "1rem",
          }}
        >
          <Button
            variant={mobileDisplay === "input" ? "contained" : "outlined"}
            onClick={() => setMobileDisplay("input")}
          >
            Input
          </Button>
          <Button
            variant={mobileDisplay === "output" ? "contained" : "outlined"}
            onClick={() => setMobileDisplay("output")}
          >
            Output
          </Button>
        </Box>
      </Box>
      <PlayGround
        code={code}
        setCode={setCode}
        wirespecResult={wirespecResult}
        wirespecOutput={wirespecOutput}
        mobileDisplay={mobileDisplay}
      />
    </Box>
  );
}