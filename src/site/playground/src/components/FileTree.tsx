import { useEffect, useState } from "react";
import { Box, ButtonBase } from "@mui/material";
import { FileGroup, fileName, groupOf } from "../utils/FileGroups";

interface FileTreeProps {
  groups: FileGroup[];
  active?: string;
  onSelect: (path: string) => void;
}

const rowSx = {
  display: "block",
  width: "100%",
  textAlign: "left",
  fontSize: 13,
  lineHeight: "24px",
  whiteSpace: "nowrap",
  overflow: "hidden",
  textOverflow: "ellipsis",
  color: "var(--color-primary)",
  "&:hover": { background: "#1a1a1a" },
} as const;

const selectedSx = {
  background: "#04395e",
  "&:hover": { background: "#04395e" },
} as const;

// The emitted files as one folder per package. Root files sit at the top level.
// Folders start collapsed, except the one holding the open file.
export function FileTree({ groups, active, onSelect }: FileTreeProps) {
  const activeLabel = groupOf(groups, active)?.label;
  const [expanded, setExpanded] = useState<string[]>([]);

  useEffect(() => {
    if (activeLabel)
      setExpanded((current) =>
        current.includes(activeLabel) ? current : [...current, activeLabel],
      );
  }, [activeLabel]);

  const toggle = (label: string) =>
    setExpanded((current) =>
      current.includes(label)
        ? current.filter((it) => it !== label)
        : [...current, label],
    );

  return (
    <Box
      component="nav"
      aria-label="Files"
      sx={{
        width: 200,
        flexShrink: 0,
        height: "100vh",
        overflowY: "auto",
        borderRight: "1px solid var(--border-primary)",
        py: 0.5,
      }}
    >
      {groups.map(({ label, files }) => {
        const open = !label || expanded.includes(label);
        return (
          <Box key={label}>
            {label && (
              <ButtonBase
                sx={{ ...rowSx, pl: 1 }}
                onClick={() => toggle(label)}
                aria-expanded={open}
              >
                {open ? "▾" : "▸"} {label}
              </ButtonBase>
            )}
            {open &&
              files.map((file) => (
                <ButtonBase
                  key={file.file}
                  onClick={() => onSelect(file.file)}
                  aria-current={file.file === active}
                  sx={{
                    ...rowSx,
                    pl: label ? 3.5 : 1,
                    ...(file.file === active && selectedSx),
                  }}
                >
                  {fileName(file.file)}
                </ButtonBase>
              ))}
          </Box>
        );
      })}
    </Box>
  );
}
