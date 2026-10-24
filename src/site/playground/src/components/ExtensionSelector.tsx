import {
  Checkbox,
  FormControl,
  ListItemText,
  MenuItem,
  Select,
  SelectChangeEvent,
} from "@mui/material";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { type Extension } from "../routes/index";
import { supports } from "../transformations/WirespecToTarget";

type Option = { value: Extension; label: string };

const extensionOptions: Option[] = [
  { value: "avro", label: "Avro" },
  { value: "jackson", label: "Jackson" },
  { value: "jvm_builder", label: "JVM builder" },
  { value: "kotest_dsl", label: "Kotest DSL" },
  { value: "kotlinx_serialization", label: "kotlinx.serialization" },
  { value: "spring_mapping_annotations", label: "Spring mapping annotations" },
  { value: "spring_native_hints", label: "Spring native hints" },
];

const labelOf = (extension: Extension) =>
  extensionOptions.find(({ value }) => value === extension)?.label;

// Only the extensions of the selected output are shown, but the others stay in
// the search, so switching back to an output brings its extensions back.
export function ExtensionSelector() {
  const navigate = useNavigate({ from: "/" });
  const { emitter, extensions = [] } = useSearch({ from: "/" });
  const options = extensionOptions.filter(({ value }) =>
    supports(value, emitter),
  );
  const selected = extensions.filter((it) => supports(it, emitter));

  const handleChange = ({ target }: SelectChangeEvent<Extension[]>) => {
    // A string when the browser autofills the select.
    const values = (
      typeof target.value === "string" ? target.value.split(",") : target.value
    ) as Extension[];
    const kept = extensions.filter((it) => !supports(it, emitter));
    navigate({
      search: (prev) => ({
        ...prev,
        // In the order of the options, which is the order they are applied in.
        extensions: extensionOptions
          .map(({ value }) => value)
          .filter((it) => kept.includes(it) || values.includes(it)),
      }),
    });
  };

  if (options.length === 0) return null;

  return (
    <FormControl
      size="small"
      sx={{
        minWidth: 120,
        // Sized like the selects in a file bar, also where it is shown outside one.
        "& .MuiInputBase-root": { height: 28, fontSize: 14 },
        "& .MuiSelect-select": { py: 0 },
      }}
    >
      <Select
        multiple
        displayEmpty
        value={selected}
        onChange={handleChange}
        renderValue={(values) =>
          values.length === 0
            ? "No extensions"
            : values.length === 1
              ? labelOf(values[0])
              : `${values.length} extensions`
        }
      >
        {options.map(({ value, label }) => (
          <MenuItem key={value} value={value} dense>
            <Checkbox
              size="small"
              checked={selected.includes(value)}
              sx={{ p: 0, mr: 1 }}
            />
            <ListItemText primary={label} />
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}
