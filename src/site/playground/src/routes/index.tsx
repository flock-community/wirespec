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
          {value: error.message, line: 1, position: 1, length: 1 },
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
    switch (specification) {
      case "wirespec":
        return setCode(wsExample);
      case "open_api_v2":
        return setCode(swaggerExample);
      case "open_api_v3":
        return setCode(openapiExample);
    }
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
    <Box display="flex">
      <Box
        flex={1}
        display={{
          xs: mobileDisplay === "input" ? "inline-block" : "none",
          sm: "block",
        }}
      >
        <Box
          marginInline={{ xs: 1, sm: 8 }}
          display="flex"
          justifyContent="space-between"
        >
          <SpecificationSelector />

          <Box display={{ sm: "none" }}>
            <Button
              sx={{ color: "var(--color-primary)" }}
              onClick={() => setMobileDisplay("output")}
            >
              show output
            </Button>
          </Box>
        </Box>
        <Box marginTop={1} borderTop="1px solid var(--border-primary)">
          <PlayGround
            code={code}
            setCode={setCode}
            language={specification === "wirespec" ? "wirespec" : "json"}
          />
        </Box>
      </Box>
      <Box
        flex={1}
        display={{
          xs: mobileDisplay === "output" ? "inline-block" : "none",
          sm: "block",
        }}
      >
        <Box
          marginInline={{ xs: 1, sm: 8 }}
          display="flex"
          justifyContent="space-between"
        >
          <EmitterSelector />
          <Box display={{ sm: "none" }}>
            <Button
              sx={{ color: "var(--color-primary)" }}
              onClick={() => setMobileDisplay("input")}
            >
              show input
            </Button>
          </Box>
        </Box>

        <Box
          marginTop={1}
          borderTop="1px solid var(--border-primary)"
          borderLeft={{ sm: "1px solid var(--border-primary)" }}
        >
          <PlayGround
            code={wirespecResult}
            language={wirespecOutput?.language || "wirespec"}
          />
        </Box>
      </Box>
    </Box>
  );
}
