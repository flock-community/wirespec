import { FormControl, MenuItem, Select } from "@mui/material";

interface TargetLanguageSelectorProps {
    selectedLanguage: string
    setSelectedLanguage: (input: string) => void
}

export function TargetLanguageSelector({selectedLanguage, setSelectedLanguage}: TargetLanguageSelectorProps) {

    const handleChange = (event: { target: { value: string; }; }) => {
        setSelectedLanguage(event.target.value);
    };


    return (
      <FormControl sx={{ minWidth: 120 }} size="small">
        <Select
          value={selectedLanguage}
          onChange={handleChange}
        >
          <MenuItem value="typescript">TypeScript</MenuItem>
          <MenuItem value="kotlin">Kotlin</MenuItem>
          <MenuItem value="scala">Scala</MenuItem>
          <MenuItem value="java">Java</MenuItem>
        </Select>
      </FormControl>
    );
}

export default TargetLanguageSelector;
