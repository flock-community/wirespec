import wirespecLogo from "./assets/wirespec.svg";
import "./App.css";
import { Box, createTheme, ThemeProvider } from "@mui/material";
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
      <Box height="100vh" display="flex" flexDirection="column" rowGap={2}>
        <Box
          component="header"
          display="flex"
          alignItems="center"
          gap={2}
          paddingBlock={2}
          paddingInline={2}
          bgcolor="#333"
        >
          <span
            style={{
              display: "flex",
              alignItems: "center",
              gap: "8px",
              fontSize: "20px",
              fontWeight: 600,
            }}
          >
            <img src={wirespecLogo} height={30} alt="Wirespec Logo" />
            <span> Wirespec Playground </span>
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
            Documentation
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

        <Box flex={1} paddingInline={2}>
          <Outlet />
        </Box>
      </Box>
    </ThemeProvider>
  );
}

export default App;
