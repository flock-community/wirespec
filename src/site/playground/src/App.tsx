import wirespecLogo from "./assets/wirespec.svg";
import "./App.css";
import { Box, createTheme, ThemeProvider } from "@mui/material";
import GitHubIcon from "@mui/icons-material/GitHub";
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
          component="header"
          display="flex"
          alignItems="center"
          gap={8}
          paddingBlock={2}
          paddingInline={2}
          bgcolor="#333"
        >
          <span
            style={{
              display: "flex",
              alignItems: "center",
              gap: "6px",
              fontSize: "24px",
              fontWeight: 700,
            }}
          >
            <img src={wirespecLogo} height={30} alt="Wirespec Logo" />
            <span> Wirespec </span>
          </span>

          <a
            href="https://github.com/flock-community/wirespec"
            target="_blank"
            title="GitHub"
            style={{ marginLeft: "auto", color: "white" }}
          >
            <GitHubIcon />
          </a>
        </Box>

        <Box paddingInline={2}>
          <Outlet />
        </Box>
      </Box>
    </ThemeProvider>
  );
}

export default App;
