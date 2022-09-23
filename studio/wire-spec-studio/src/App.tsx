import wireSpec from "wire-spec-lib";
import { Box, Container, CssBaseline, TextareaAutosize } from "@mui/material";
import { useEffect, useState } from "react";

function App() {
  const wsToTs = new wireSpec.WsToTypeScript();

  const [src, setSrc] = useState<string>("type Todo {\n  done: Boolean\n}");
  const [out, setOut] = useState<string>();

  useEffect(() => {
    const res = wsToTs.compile(src);
    if (res.compiled) {
      setOut(res.compiled.value);
    }
  }, [src]);

  return (
    <>
      <CssBaseline />
      <Container maxWidth="sm">
        <Box display="flex">
          <Box>
            <TextareaAutosize
              value={src}
              onChange={(ev) => setSrc(ev.target.value)}
            />
          </Box>
          <Box>{out}</Box>
        </Box>
      </Container>
    </>
  );
}

export default App;
