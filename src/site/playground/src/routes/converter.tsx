import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useMonaco } from "@monaco-editor/react";
import { initializeMonaco } from "../utils/InitializeMonaco";
import {
  openApiV2ToWirespec,
  openApiV3ToWirespec,
} from "../transformations/OpenAPIToWirespec";
import { PlayGroundInput } from "../components/PlayGroundInput";
import { PlayGroundOutput } from "../components/PlayGroundOutput";
import { Grid, Typography } from "@mui/material";
import { swaggerExample } from "../examples/swagger";

export const Route = createFileRoute("/converter")({
  component: RouteComponent,
});

function RouteComponent() {
  const monaco = useMonaco();
  const [code, setCode] = useState(swaggerExample());
  useEffect(() => {
    if (!monaco) {
      return;
    }
    initializeMonaco(monaco);
  }, [monaco]);
  const [wirespecOutput, setWirespecOutput] = useState("");
  const [error, setError] = useState("");
  useEffect(() => {
    try {
      const json = JSON.parse(code);
      let compiled;
      if (json.swagger) {
        compiled = openApiV2ToWirespec(code);
      } else if (json.openapi) {
        compiled = openApiV3ToWirespec(code);
      } else {
        setError(
          `Invalid OpenAPI specification; missing 'swagger' or 'openapi' property`,
        );
        return;
      }
      setWirespecOutput(compiled[0].result);
      setError("");
    } catch (error) {
      let message;
      if (error instanceof Error) message = error.message;
      else message = String(error);
      setError(message);
    }
  }, [code]);
  return (
    <>
      <Grid container spacing={1}>
        <Grid item xs={6} alignContent="center">
          <Typography variant="h5">OpenAPI (JSON)</Typography>
        </Grid>
        <Grid item xs={6} alignContent="center">
          <Typography variant="h5">Wirespec</Typography>
        </Grid>

        <Grid item xs={12} md={6}>
          <PlayGroundInput code={code} setCode={setCode} />
        </Grid>

        <Grid item xs={12} md={6}>
          <PlayGroundOutput code={wirespecOutput} language="wirespec" />
        </Grid>

        <Grid item xs={6}>
          <Typography variant="h5">{error ? error : "No errors"}</Typography>
        </Grid>
        <Grid item xs={6} />
      </Grid>
    </>
  );
}
