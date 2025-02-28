import { FormControl, MenuItem, Select } from "@mui/material";

interface TargetLanguageSelectorProps {
  selectedLanguage: string;
  setSelectedLanguage: (input: string) => void;
}

export function TargetLanguageSelector({
  selectedLanguage,
  setSelectedLanguage,
}: TargetLanguageSelectorProps) {
  const handleChange = (event: { target: { value: string } }) => {
    setSelectedLanguage(event.target.value);
  };

  return (
    <FormControl sx={{ minWidth: 120 }} size="small">
      <Select value={selectedLanguage} onChange={handleChange}>
        <MenuItem value="typescript">TypeScript</MenuItem>
        <MenuItem value="kotlin">Kotlin</MenuItem>
        <MenuItem value="scala">Scala</MenuItem>
        <MenuItem value="java">Java</MenuItem>
        <MenuItem value="open_api_v2">OpenApi v2</MenuItem>
        <MenuItem value="open_api_v3">OpenApi v3</MenuItem>
        <MenuItem value="avro">Avro</MenuItem>
      </Select>
    </FormControl>
  );
}

export default TargetLanguageSelector;
