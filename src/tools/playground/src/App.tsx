import wirespecLogo from "./assets/wirespec.svg";
import "./App.css";
import { Box, createTheme, ThemeProvider, Typography } from "@mui/material";
import "@fontsource/roboto/300.css";
import "@fontsource/roboto/400.css";
import "@fontsource/roboto/500.css";
import "@fontsource/roboto/700.css";
import { Outlet, Link } from "@tanstack/react-router";

const darkTheme = createTheme({
  palette: {
    mode: "dark",
  },
});

function App() {
  return (
    <ThemeProvider theme={darkTheme}>
      <Box display="flex" flexDirection="column" rowGap={2}>
        <Box
          display="flex"
          alignItems="center"
          gap={8}
          paddingBlock={2}
          paddingInline={2}
          bgcolor="#333"
        >
          <a href="https://github.com/flock-community/wirespec" target="_blank">
            <img src={wirespecLogo} height={50} alt="Wirespec Logo" />
          </a>

          <Box display="flex" gap={4}>
            <Link
              to="/compiler"
              search={{ emitter: "typescript" }}
              activeOptions={{ includeSearch: false }}
              activeProps={{
                style: {
                  color: "#646cff",
                },
              }}
            >
              <Typography>Compiler</Typography>
            </Link>
            <Link
              to="/converter"
              activeProps={{
                style: {
                  color: "#646cff",
                },
              }}
            >
              <Typography>Converter</Typography>
            </Link>
          </Box>
        </Box>

        <Box paddingInline={2}>
          <Outlet />
        </Box>
      </Box>
    </ThemeProvider>
  );
}

export default App;
