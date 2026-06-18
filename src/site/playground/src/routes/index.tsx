import { createFileRoute } from "@tanstack/react-router";
import { useSearch } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { WsError, WsEmitted } from "@flock/wirespec";
import { useMonaco } from "@monaco-editor/react";
import { Box, Button, Tab, Tabs } from "@mui/material";
import { PlayGround } from "../components/PlayGround";
import { OutputView } from "../components/OutputView";
import { SpecificationSelector } from "../components/SpecificationSelector";
import { EmitterSelector } from "../components/EmitterSelector";
import { PackageSelector, packageOf } from "../components/PackageSelector";
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
  const [wirespecErrors, setWirespecErrors] = useState<MonacoError[]>([]);
  const [selectedPackage, setSelectedPackage] = useState<string>();
  const [mobileDisplay, setMobileDisplay] = useState<"input" | "output">(
    "input",
  );

  const files = wirespecOutput?.result ?? [];

  // Distinct packages (the directory containing each file) across the emitted
  // files, in first-seen order, used to populate the package selector.
  const packages = useMemo(() => {
    const seen = new Set<string>();
    files.forEach((file) => seen.add(packageOf(file.file)));
    return [...seen];
  }, [files]);

  // Default to the first package, and fall back to it when the chosen package
  // is absent from the current output (e.g. after switching emitter/spec).
  const activePackage =
    selectedPackage && packages.includes(selectedPackage)
      ? selectedPackage
      : packages[0];

  const filteredFiles = files.filter(
    (file) => packageOf(file.file) === activePackage,
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
    if (wirespecOutput?.errors) {
      setWirespecErrors(wirespecOutput.errors);
    }
  }, [wirespecOutput]);

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
          marginInline={{ xs: 1, sm: 2 }}
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
          {/* Single file tab mirroring the output panel's file tabs, so both
              editors stay vertically aligned. */}
          <Tabs
            value={0}
            sx={{
              minHeight: 36,
              borderBottom: "1px solid var(--border-primary)",
              "& .MuiTab-root": {
                color: "var(--color-primary)",
                minHeight: 36,
                textTransform: "none",
              },
            }}
          >
            <Tab
              value={0}
              label={specification === "wirespec" ? "todo.ws" : "todo.json"}
            />
          </Tabs>
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
          marginInline={{ xs: 1, sm: 2 }}
          display="flex"
          justifyContent="space-between"
        >
          <Box display="flex" gap={1}>
            <EmitterSelector />
            {packages.length > 1 && (
              <PackageSelector
                packages={packages}
                value={activePackage ?? ""}
                onChange={setSelectedPackage}
              />
            )}
          </Box>
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
          <OutputView
            files={filteredFiles}
            language={wirespecOutput?.language || "wirespec"}
          />
        </Box>
      </Box>
    </Box>
  );
}
