import wirespecLogo from "./assets/wirespec.svg";
import "./App.css";
import { Box, createTheme, Stack, ThemeProvider } from "@mui/material";
import "@fontsource/roboto/300.css";
import "@fontsource/roboto/400.css";
import "@fontsource/roboto/500.css";
import "@fontsource/roboto/700.css";
import { Outlet } from "@tanstack/react-router";

const darkTheme = createTheme({
  palette: {
    mode: "dark",
  },
});

function App() {
  return (
    <ThemeProvider theme={darkTheme}>
      <Box
        height={{ sm: "100vh" }}
        overflow="hidden"
        display="flex"
        flexDirection="column"
        rowGap={1}
      >
        <Box
          component="header"
          display="flex"
          alignItems="center"
          gap={2}
          paddingBlock={2}
          paddingInline={{ xs: 1, sm: 2 }}
          borderBottom="1px solid var(--border-primary)"
        >
          <span
            style={{
              display: "flex",
              alignItems: "center",
              gap: "8px",
            }}
          >
            <img src={wirespecLogo} height={30} alt="" />
            <Stack fontSize={{ sm: "20px" }} fontWeight={600}>
              Wirespec Playground
            </Stack>
          </span>

          <a
            href="https://docs.wirespec.io/"
            target="_blank"
            rel="noopener"
            style={{
              marginLeft: "auto",
              color: "white",
              textDecoration: "none",
            }}
          >
            Docs
          </a>

          <a
            href="https://github.com/flock-community/wirespec"
            target="_blank"
            rel="noopener"
            style={{ color: "white", textDecoration: "none" }}
          >
            GitHub
          </a>
        </Box>

        <Outlet />
      </Box>
    </ThemeProvider>
  );
}

export default App;
