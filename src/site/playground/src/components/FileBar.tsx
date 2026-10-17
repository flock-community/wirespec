import { ReactNode } from "react";
import { Box } from "@mui/material";

// The bar above each editor. Both sides use it so the editors line up.
export function FileBar({
  children,
  controls,
}: {
  children: ReactNode;
  controls?: ReactNode;
}) {
  return (
    <Box
      sx={{
        display: "flex",
        alignItems: "center",
        gap: 0.5,
        height: 36,
        px: 1,
        borderBottom: "1px solid var(--border-primary)",
        fontSize: 14,
        whiteSpace: "nowrap",
        overflow: "hidden",
      }}
    >
      {children}
      {controls && (
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            gap: 1,
            flexShrink: 0,
            ml: "auto",
            // A small select is taller than the bar.
            "& .MuiInputBase-root": { fontSize: 14 },
            "& .MuiSelect-select": { py: 0.5 },
          }}
        >
          {controls}
        </Box>
      )}
    </Box>
  );
}
