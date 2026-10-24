import { FormControl, ListSubheader, MenuItem, Select } from "@mui/material";
import { useNavigate, useSearch } from "@tanstack/react-router";
import {
  type CompilerEmitter,
  type ConverterEmitter,
  type Emitter,
} from "../routes/index";

type Option<T extends Emitter> = { value: T; label: string };

const toMenuItem = ({ value, label }: Option<Emitter>) => (
  <MenuItem key={value} value={value}>
    {label}
  </MenuItem>
);

export function EmitterSelector() {
  const emitterOptions: Option<CompilerEmitter>[] = [
    { value: "kotlin", label: "Kotlin" },
    { value: "java", label: "Java" },
    { value: "scala", label: "Scala" },
    { value: "typescript", label: "TypeScript" },
    { value: "python", label: "Python" },
  ];
  const converterOptions: Option<CompilerEmitter>[] = [
    { value: "open_api_v2", label: "OpenAPI v2" },
    { value: "open_api_v3", label: "OpenAPI v3" },
    { value: "avro", label: "Avro JSON" },
    { value: "avro_idl", label: "Avro IDL" },
    { value: "graphql", label: "GraphQL" },
    { value: "asyncapi", label: "AsyncAPI" },
  ];
  const wirespecOption: Option<ConverterEmitter> = {
    value: "wirespec",
    label: "Wirespec",
  };
  const navigate = useNavigate({ from: "/" });
  const { emitter, specification } = useSearch({ from: "/" });
  const handleChange = (event: { target: { value: string } }) =>
    navigate({
      search: (prev) => ({ ...prev, emitter: event.target.value as Emitter }),
    });

  // Select needs its items as direct children, so the groups are an array rather than a Fragment.
  return (
    <FormControl sx={{ minWidth: 120 }} size="small">
      <Select value={emitter} onChange={handleChange}>
        {specification === "wirespec"
          ? [
              <ListSubheader key="emitters">Emitters</ListSubheader>,
              ...emitterOptions.map(toMenuItem),
              <ListSubheader key="converters">Converters</ListSubheader>,
              ...converterOptions.map(toMenuItem),
            ]
          : toMenuItem(wirespecOption)}
      </Select>
    </FormControl>
  );
}
