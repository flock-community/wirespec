import { FormControl, MenuItem, Select } from "@mui/material";
import { useNavigate, useSearch } from "@tanstack/react-router";
import {
  type CompilerEmitter,
  type ConverterEmitter,
  type Emitter,
} from "../routes/index";
import pkg from "../../package.json";

export function EmitterSelector() {
  const compilerOptions: { value: CompilerEmitter; label: string }[] = [
    { value: "typescript", label: "TypeScript" },
    { value: "kotlin", label: "Kotlin" },
    { value: "python", label: "Python" },
    { value: "java", label: "Java" },
    { value: "open_api_v2", label: "OpenAPI v2" },
    { value: "open_api_v3", label: "OpenAPI v3" },
    { value: "avro", label: "Avro" },
  ];
  const converterOption: { value: ConverterEmitter; label: string } = {
    value: "wirespec",
    label: `Wirespec v${pkg.version}`,
  };
  const navigate = useNavigate({ from: "/" });
  const { emitter, specification } = useSearch({ from: "/" });
  const handleChange = (event: { target: { value: string } }) =>
    navigate({
      search: () => ({ emitter: event.target.value as Emitter, specification }),
    });

  return (
    <FormControl sx={{ minWidth: 120 }} size="small">
      <Select value={emitter} onChange={handleChange}>
        {specification === "wirespec" ? (
          compilerOptions.map(({ value, label }) => (
            <MenuItem key={value} value={value}>
              {label}
            </MenuItem>
          ))
        ) : (
          <MenuItem key={converterOption.value} value={converterOption.value}>
            {converterOption.label}
          </MenuItem>
        )}
      </Select>
    </FormControl>
  );
}
