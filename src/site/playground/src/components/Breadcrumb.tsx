import { MouseEvent, ReactNode, useState } from "react";
import { Box, ButtonBase, Menu, MenuItem } from "@mui/material";
import { FileGroup, fileName, groupOf } from "../utils/FileGroups";
import { FileBar } from "./FileBar";

interface BreadcrumbProps {
  prefix: string;
  groups: FileGroup[];
  active?: string;
  onSelect: (path: string) => void;
  treeOpen: boolean;
  onToggleTree: () => void;
}

const buttonSx = {
  flexShrink: 0,
  height: 26,
  px: 0.75,
  borderRadius: 1,
  fontSize: 14,
  color: "var(--color-primary)",
  "&:hover": { background: "#1f1f1f" },
} as const;

const Crumb = ({
  children,
  onClick,
}: {
  children: ReactNode;
  onClick: (event: MouseEvent<HTMLElement>) => void;
}) => (
  <ButtonBase onClick={onClick} sx={buttonSx}>
    {children}
    <Box component="span" sx={{ opacity: 0.6, ml: 0.5 }}>
      ▾
    </Box>
  </ButtonBase>
);

const Separator = () => (
  <Box component="span" sx={{ flexShrink: 0, opacity: 0.4 }}>
    ›
  </Box>
);

const TreeIcon = () => (
  <svg
    width="16"
    height="16"
    viewBox="0 0 16 16"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.3"
    aria-hidden
  >
    <rect x="1.5" y="2.5" width="13" height="11" rx="1.5" />
    <line x1="6" y1="2.5" x2="6" y2="13.5" />
  </svg>
);

const groupLabel = (group?: FileGroup) => group?.label || "root";

// The path of the open file, where the package and the file each open a menu
// to switch to another one.
export function Breadcrumb({
  prefix,
  groups,
  active,
  onSelect,
  treeOpen,
  onToggleTree,
}: BreadcrumbProps) {
  const [menu, setMenu] = useState<{
    anchor: HTMLElement;
    kind: "package" | "file";
  }>();
  const activeGroup = groupOf(groups, active);
  const close = () => setMenu(undefined);
  const select = (path: string) => {
    onSelect(path);
    close();
  };

  return (
    <FileBar>
      <ButtonBase
        onClick={onToggleTree}
        aria-label={treeOpen ? "Hide file tree" : "Show file tree"}
        aria-pressed={treeOpen}
        sx={{ ...buttonSx, opacity: treeOpen ? 1 : 0.6 }}
      >
        <TreeIcon />
      </ButtonBase>
      {prefix && (
        <>
          <Box
            component="span"
            sx={{
              minWidth: 0,
              overflow: "hidden",
              textOverflow: "ellipsis",
              opacity: 0.5,
              px: 0.75,
            }}
          >
            {prefix}
          </Box>
          <Separator />
        </>
      )}
      <Crumb
        onClick={(event) =>
          setMenu({ anchor: event.currentTarget, kind: "package" })
        }
      >
        {groupLabel(activeGroup)}
      </Crumb>
      <Separator />
      <Crumb
        onClick={(event) =>
          setMenu({ anchor: event.currentTarget, kind: "file" })
        }
      >
        {active && fileName(active)}
      </Crumb>
      <Menu anchorEl={menu?.anchor} open={!!menu} onClose={close}>
        {menu?.kind === "package"
          ? groups.map((group) => (
              <MenuItem
                key={group.label}
                dense
                selected={group === activeGroup}
                onClick={() => select(group.files[0].file)}
              >
                {groupLabel(group)}
                <Box component="span" sx={{ opacity: 0.5, ml: 2 }}>
                  {group.files.length}
                </Box>
              </MenuItem>
            ))
          : activeGroup?.files.map((file) => (
              <MenuItem
                key={file.file}
                dense
                selected={file.file === active}
                onClick={() => select(file.file)}
              >
                {fileName(file.file)}
              </MenuItem>
            ))}
      </Menu>
    </FileBar>
  );
}
