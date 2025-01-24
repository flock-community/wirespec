import wirespecLogo from "./assets/wirespec.svg"
import './App.css'
import {TabRoot} from './components/TabRoot'
import { Box, createTheme, Link, ThemeProvider, Typography } from "@mui/material"
import '@fontsource/roboto/300.css';
import '@fontsource/roboto/400.css';
import '@fontsource/roboto/500.css';
import '@fontsource/roboto/700.css';

const darkTheme = createTheme({
  palette: {
    mode: 'dark',
    primary: {
      main: '#fcdf00'
    },
    secondary: {
      main: '#ffffff'
    }
  },
});

darkTheme.typography.h1 = {
  fontSize: '6vw',
  fontFamily: 'TechnaSans-Regular'
};

darkTheme.typography.h5 = {
  fontFamily: 'Roboto'
};

function App() {
    return (
      <ThemeProvider theme={darkTheme}>
        <Box sx={{marginLeft: 10, marginRight: 10, display: "flex", flexDirection: "column"}}>
          <Box sx={{width: '100%', display: "flex", justifyContent: "center"}}>
            <Typography variant="h1">
              <Box sx={{display: "flex", alignItems: "center", gap: "10px"}}>
                WIRESPEC
                <Link href="https://github.com/flock-community/wirespec" target="_blank">
                  <img src={wirespecLogo} className="logo" alt="Wirespec Logo"/>
                </Link>
                PLAYGROUND
              </Box>
            </Typography>
            </Box>
          <Box>
            <TabRoot />
          </Box>
        </Box>
      </ThemeProvider>
    )
}

export default App
