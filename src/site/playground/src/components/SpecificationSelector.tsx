import { FormControl, ListSubheader, MenuItem, Select } from "@mui/material";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { type Specification } from "../routes/index";

type CompileOption = { value: Specification; label: string };

export function SpecificationSelector() {
  const compileOptions: CompileOption[] = [
    { value: "wirespec", label: "Wirespec" },
  ];

  const convertOptions: CompileOption[] = [
    { value: "open_api_v2", label: "Open API v2" },
    { value: "open_api_v3", label: "Open API v3" },
  ];

  const navigate = useNavigate({ from: "/" });
  const { specification, emitter } = useSearch({ from: "/" });

  const handleChange = (event: { target: { value: string } }) => {
    if (event.target.value === "wirespec") {
      return navigate({
        search: () => ({ specification: "wirespec", emitter }),
      });
    }

    return navigate({
      search: () => ({
        specification: event.target.value as Specification,
        emitter: "wirespec",
      }),
    });
  };

  return (
    <FormControl sx={{ minWidth: 120 }} size="small">
      <Select value={specification} onChange={handleChange}>
        <ListSubheader>Compile options</ListSubheader>
        {compileOptions.map((option) => (
          <MenuItem value={option.value}>{option.label}</MenuItem>
        ))}

        <ListSubheader>Convert options</ListSubheader>
        {convertOptions.map((option) => (
          <MenuItem value={option.value}>{option.label}</MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}
