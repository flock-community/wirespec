import { FormControl, MenuItem, Select } from "@mui/material";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { type Output } from "../routes";

export function TargetLanguageSelector() {
  const options: { value: Output; label: string }[] = [
    { value: "typescript", label: "TypeScript" },
    { value: "kotlin", label: "Kotlin" },
    { value: "scala", label: "Scala" },
    { value: "java", label: "Java" },
    { value: "open_api_v2", label: "OpenApi v2" },
    { value: "open_api_v3", label: "OpenApi v3" },
    { value: "avro", label: "Avro" },
  ];

  const navigate = useNavigate({ from: "/" });
  const { output } = useSearch({ from: "/" });

  const handleChange = (event: { target: { value: string } }) => {
    navigate({ search: () => ({ output: event.target.value as Output }) });
  };

  return (
    <FormControl sx={{ minWidth: 120 }} size="small">
      <Select value={output} onChange={handleChange}>
        {options.map((option) => (
          <MenuItem value={option.value}>{option.label}</MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}

export default TargetLanguageSelector;
