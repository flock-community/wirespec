import { createFileRoute } from "@tanstack/react-router";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { useEffect, useRef, useState } from "react";
import { WsError, WsEmitted } from "@flock/wirespec";
import { useMonaco } from "@monaco-editor/react";
import { Box, Button } from "@mui/material";
import { PlayGround } from "../components/PlayGround";
import { OutputView } from "../components/OutputView";
import { SpecificationSelector } from "../components/SpecificationSelector";
import { EmitterSelector } from "../components/EmitterSelector";
import { ExtensionSelector } from "../components/ExtensionSelector";
import { FileBar } from "../components/FileBar";
import { initializeMonaco } from "../utils/InitializeMonaco";
import { MonacoError, setMonacoErrors } from "../utils/SetMonacoErrors";
import { decodeCode, encodeCode } from "../utils/ShareCode";
import {
  isExtension,
  wirespecToTarget,
} from "../transformations/WirespecToTarget";
import {
  wsExample,
  wsGraphQLExample,
  wsAsyncAPIExample,
} from "../examples/wirespec";
import { swaggerExample } from "../examples/swagger";
import {
  openApiV2ToWirespec,
  openApiV3ToWirespec,
  avroToWirespec,
  graphqlToWirespec,
  asyncapiToWirespec,
} from "../transformations/OpenAPIToWirespec";
import { openapiExample } from "../examples/openapi";
import { avroExample } from "../examples/avro";
import { graphqlExample } from "../examples/graphql";
import { asyncapiExample } from "../examples/asyncapi";

type CompileSpecification = "wirespec";
type ConvertSpecification =
  | "open_api_v2"
  | "open_api_v3"
  | "avro"
  | "graphql"
  | "asyncapi";

export type Specification = CompileSpecification | ConvertSpecification;

export type CompilerEmitter =
  | "kotlin"
  | "java"
  | "scala"
  | "typescript"
  | "python"
  | "open_api_v2"
  | "open_api_v3"
  | "avro"
  | "avro_idl"
  | "graphql"
  | "asyncapi";
export type ConverterEmitter = "wirespec";
export type Emitter = CompilerEmitter | ConverterEmitter;

export type Extension =
  | "avro"
  | "jackson"
  | "jvm_builder"
  | "kotest_dsl"
  | "kotlinx_serialization"
  | "spring_mapping_annotations"
  | "spring_native_hints";

export type Language =
  | "wirespec"
  | "kotlin"
  | "java"
  | "scala"
  | "typescript"
  | "python"
  | "json"
  | "graphql"
  | "avdl";

type Search = {
  specification: Specification;
  emitter: Emitter;
  extensions?: Extension[];
  // Only when edited, so a link to an example follows changes to it.
  code?: string;
  file?: string;
  tree?: boolean;
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
      extensions: Array.isArray(search?.extensions)
        ? search?.extensions.filter(isExtension)
        : undefined,
      code: typeof search?.code === "string" ? search.code : undefined,
      file: typeof search?.file === "string" ? search.file : undefined,
      tree: search?.tree === true || undefined,
    };
  },
});

const exampleFor = (specification: Specification, emitter: Emitter) => {
  switch (specification) {
    case "wirespec":
      return emitter === "graphql"
        ? wsGraphQLExample
        : emitter === "asyncapi"
          ? wsAsyncAPIExample
          : wsExample;
    case "open_api_v2":
      return swaggerExample;
    case "open_api_v3":
      return openapiExample;
    case "avro":
      return avroExample;
    case "graphql":
      return graphqlExample;
    case "asyncapi":
      return asyncapiExample;
  }
};

const examples = [
  wsExample,
  wsGraphQLExample,
  wsAsyncAPIExample,
  swaggerExample,
  openapiExample,
  avroExample,
  graphqlExample,
  asyncapiExample,
];

function RouteComponent() {
  const monaco = useMonaco();
  const navigate = useNavigate({ from: "/" });
  const {
    emitter,
    specification,
    extensions,
    code: linkedCode,
  } = useSearch({ from: "/" });
  // Undefined until the code in the link is decoded.
  const [code, setCode] = useState<string>();
  // What the link held when the code was last read from or written to it, so
  // the page does not read back its own writes.
  const linked = useRef<{ specification: Specification; code?: string }>(
    undefined,
  );
  const [wirespecOutput, setWirespecOutput] = useState<CompilationResult>();
  const [wirespecErrors, setWirespecErrors] = useState<MonacoError[]>([]);
  const [mobileDisplay, setMobileDisplay] = useState<"input" | "output">(
    "input",
  );

  useEffect(() => {
    if (code === undefined) return;
    if (specification === "wirespec") {
      const compiledOutput = wirespecToTarget(code, emitter, extensions ?? []);
      setWirespecOutput(compiledOutput);
      return;
    }

    try {
      // GraphQL is not JSON, so it is converted before the JSON specifications are told apart.
      if (specification === "graphql") {
        setWirespecOutput(graphqlToWirespec(code));
        return setWirespecErrors([]);
      }

      const json = JSON.parse(code);

      if (specification === "avro") {
        setWirespecOutput(avroToWirespec(code));
      } else if (specification === "asyncapi") {
        setWirespecOutput(asyncapiToWirespec(code));
      } else if (json.swagger) {
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
        // The GraphQL converter reports where it failed: "... at line 3, column 7".
        const [, line, column] =
          /at line (\d+), column (\d+)/.exec(error.message) ?? [];
        setWirespecErrors([
          {
            value: error.message,
            line: Number(line ?? 1),
            position: Number(column ?? 1),
            length: 1,
          },
        ]);
      }
    }
  }, [code, emitter, specification, extensions]);

  useEffect(() => {
    if (!monaco) return;
    initializeMonaco(monaco);
  }, [monaco]);

  useEffect(() => {
    if (!monaco) return;

    setMonacoErrors(monaco, wirespecErrors);
  }, [wirespecErrors, monaco]);

  // On load, on switching specifications and on going back or forward.
  useEffect(() => {
    if (
      linked.current?.specification === specification &&
      linked.current.code === linkedCode
    )
      return;
    linked.current = { specification, code: linkedCode };
    const example = exampleFor(specification, emitter);
    if (!linkedCode) return setCode(example);
    decodeCode(linkedCode).then(setCode, () => setCode(example));
  }, [specification, linkedCode]);

  useEffect(() => {
    if (code === undefined) return;
    let cancelled = false;
    (code === exampleFor(specification, emitter)
      ? Promise.resolve(undefined)
      : encodeCode(code)
    ).then((encoded) => {
      if (cancelled || encoded === linked.current?.code) return;
      linked.current = { specification, code: encoded };
      navigate({
        replace: true,
        search: (prev) => ({ ...prev, code: encoded }),
      });
    });
    return () => {
      cancelled = true;
    };
  }, [code]);

  // Only an unedited example follows the output, so switching outputs never loses changes.
  useEffect(() => {
    setCode((current) =>
      current !== undefined && examples.includes(current)
        ? exampleFor(specification, emitter)
        : current,
    );
  }, [emitter]);

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
          display={{ xs: "flex", sm: "none" }}
          justifyContent="flex-end"
          height={36}
          borderBottom="1px solid var(--border-primary)"
        >
          <Button
            sx={{ color: "var(--color-primary)" }}
            onClick={() => setMobileDisplay("output")}
          >
            show output
          </Button>
        </Box>
        <FileBar controls={<SpecificationSelector />}>
          <Box component="span" px={0.75}>
            {specification === "wirespec"
              ? "todo.ws"
              : specification === "graphql"
                ? "todo.graphql"
                : "todo.json"}
          </Box>
        </FileBar>
        <PlayGround
          code={code ?? ""}
          setCode={setCode}
          language={
            specification === "wirespec" || specification === "graphql"
              ? specification
              : "json"
          }
        />
      </Box>
      <Box
        flex={1}
        display={{
          xs: mobileDisplay === "output" ? "inline-block" : "none",
          sm: "block",
        }}
      >
        <Box
          display={{ xs: "flex", sm: "none" }}
          justifyContent="flex-end"
          alignItems="center"
          height={36}
          pl={1}
          borderBottom="1px solid var(--border-primary)"
        >
          {/* A phone has no room for it next to the file path. */}
          <ExtensionSelector />
          <Button
            sx={{ color: "var(--color-primary)", ml: "auto" }}
            onClick={() => setMobileDisplay("input")}
          >
            show input
          </Button>
        </Box>

        <OutputView
          files={wirespecOutput?.result ?? []}
          language={wirespecOutput?.language || "wirespec"}
          controls={
            <>
              <EmitterSelector />
              <Box display={{ xs: "none", sm: "contents" }}>
                <ExtensionSelector />
              </Box>
            </>
          }
        />
      </Box>
    </Box>
  );
}
