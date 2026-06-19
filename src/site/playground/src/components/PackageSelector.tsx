import { FormControl, MenuItem, Select } from "@mui/material";

interface PackageSelectorProps {
  packages: string[];
  value: string;
  onChange: (pkg: string) => void;
}

// Render the package path as a dotted name (e.g.
// "community/flock/wirespec/generated/model" -> "community.flock.wirespec.generated.model").
// Files emitted at the root (e.g. "Wirespec.ts") have no package; label them "root".
const packageLabel = (pkg: string) => (pkg ? pkg.replace(/\//g, ".") : "root");

export function PackageSelector({
  packages,
  value,
  onChange,
}: PackageSelectorProps) {
  return (
    <FormControl sx={{ minWidth: 120 }} size="small">
      <Select
        value={value}
        onChange={(event) => onChange(event.target.value)}
      >
        {packages.map((pkg) => (
          <MenuItem key={pkg} value={pkg}>
            {packageLabel(pkg)}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}

// The package of an emitted file is its full directory path (every path
// segment except the file name), so it captures the complete package:
// "/model/Todo.ts" -> "model" and
// "community/flock/.../model/Todo.kt" -> "community/flock/.../model".
// Root-level files (e.g. "Wirespec.ts") have no package and map to "".
export const packageOf = (path: string) =>
  path.split("/").filter(Boolean).slice(0, -1).join("/");
