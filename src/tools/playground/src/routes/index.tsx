import { createFileRoute } from "@tanstack/react-router";
import App from "../App";

export type Output =
  | "typescript"
  | "kotlin"
  | "scala"
  | "java"
  | "open_api_v2"
  | "open_api_v3"
  | "avro";

type Search = {
  output: Output;
};

export const Route = createFileRoute("/")({
  component: Index,
  validateSearch: (search: Record<string, unknown>): Search => {
    return {
      output: (search?.output as Search["output"]) || "typescript",
    };
  },
});

function Index() {
  return <App />;
}
