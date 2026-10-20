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
        // The header's gutter, less the 6px the text sits inside its hover
        // area, so the text and the select line up with the header.
        pl: { xs: 0.25, sm: 1.25 },
        pr: { xs: 1, sm: 2 },
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
            // A small select is taller than the bar, and a whole number of
            // pixels keeps its outline sharp.
            "& .MuiInputBase-root": { height: 28, fontSize: 14 },
            "& .MuiSelect-select": { py: 0 },
          }}
        >
          {controls}
        </Box>
      )}
    </Box>
  );
}
