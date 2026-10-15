import { ReactNode } from "react";
import { Box } from "@mui/material";

// The bar above each editor. Both sides use it so the editors line up.
export function FileBar({ children }: { children: ReactNode }) {
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
    </Box>
  );
}
