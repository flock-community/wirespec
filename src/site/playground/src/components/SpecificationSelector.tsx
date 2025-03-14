import { FormControl, ListSubheader, MenuItem, Select } from "@mui/material";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { type Specification } from "../routes/compiler";

export function SpecificationSelector() {
  const compileOptions: { value: Specification; label: string }[] = [
    { value: "wirespec", label: "Wirespec" },
  ];

  // TODO
  const convertOptions: { value: string; label: string }[] = [
    { value: "open_api_v2", label: "Open API v2" },
    { value: "open_api_v3", label: "Open API v3" },
  ];

  const navigate = useNavigate({ from: "/compiler" });
  const { specification } = useSearch({ from: "/compiler" });

  const handleChange = (event: { target: { value: string } }) => {
    if (event.target.value === "wirespec") {
      return navigate({
        search: () => ({ specification: event.target.value as Specification }),
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
