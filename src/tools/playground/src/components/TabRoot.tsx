import { SyntheticEvent, useState} from "react";
import { Box, Tab } from '@mui/material';
import { TabContext, TabList, TabPanel } from '@mui/lab';
import { PlayGround } from './PlayGround';
import { OpenApiConverter } from './OpenApiConverter';
import { wsExample } from '../examples/wirespec';
import { swaggerExample } from "../examples/swagger";

export function TabRoot() {
  const [code, setCode] = useState(wsExample());
  const [selectedLanguage, setSelectedLanguage] = useState('typescript');
  const [spec, setSpec] = useState(swaggerExample());
  const [value, setValue] = useState('1');

  const handleTabChange = (event: SyntheticEvent, newValue: string) => {
    setValue(newValue);
  }

  return (
    <TabContext value={value}>
      <Box>
        <TabList
          sx={{"& .MuiTabs-indicator": { display: "none" }}}
          textColor='primary'
          aria-label='Wirespec tabs'
          onChange={handleTabChange}
        >
          <Tab label="Wirespec compiler" value='1'/>
          <Tab label="OpenAPI converter" value='2'/>
        </TabList>
      </Box>
      <TabPanel value='1' sx={{border: 1, padding: 1, borderColor: '#fcdf00'}}>
        <PlayGround
          code={code}
          setCode={setCode}
          selectedLanguage={selectedLanguage}
          setSelectedLanguage={setSelectedLanguage}>
        </PlayGround>
      </TabPanel>
      <TabPanel value='2' sx={{border: 1, padding: 1, borderColor: '#fcdf00'}}>
        <OpenApiConverter code={spec} setCode={setSpec}></OpenApiConverter>
      </TabPanel>
    </TabContext>
  );
}
