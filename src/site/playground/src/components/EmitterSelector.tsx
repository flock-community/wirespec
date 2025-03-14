import { FormControl, MenuItem, Select } from "@mui/material";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { type Emitter } from "../routes/compiler";

export function EmitterSelector() {
  const options: { value: Emitter; label: string }[] = [
    { value: "typescript", label: "TypeScript" },
    { value: "kotlin", label: "Kotlin" },
    { value: "scala", label: "Scala" },
    { value: "java", label: "Java" },
    { value: "open_api_v2", label: "OpenAPI v2" },
    { value: "open_api_v3", label: "OpenAPI v3" },
    { value: "avro", label: "Avro" },
  ];

  const navigate = useNavigate({ from: "/compiler" });
  const { emitter } = useSearch({ from: "/compiler" });

  const handleChange = (event: { target: { value: string } }) => {
    navigate({ search: () => ({ emitter: event.target.value as Emitter }) });
  };

  return (
    <FormControl sx={{ minWidth: 120 }} size="small">
      <Select value={emitter} onChange={handleChange}>
        {options.map((option) => (
          <MenuItem value={option.value}>{option.label}</MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}
