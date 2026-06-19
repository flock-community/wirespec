import { useMemo, useState } from "react";
import { Box, Tab, Tabs } from "@mui/material";
import { WsEmitted } from "@flock/wirespec";
import { PlayGround } from "./PlayGround";
import { Language } from "../routes";

interface OutputViewProps {
  files: WsEmitted[];
  language: Language;
}

// Derive a short, readable tab label from an emitted file path
// (e.g. "/model/TodoIdentifier.ts" -> "TodoIdentifier.ts").
const fileLabel = (path: string) => path.split("/").pop() || path;

export function OutputView({ files, language }: OutputViewProps) {
  const [selected, setSelected] = useState<string>();

  // Resolve the currently selected file by path so the selection survives
  // recompilation (which produces a fresh `files` array on every edit). When
  // the selected file no longer exists, fall back to the first file.
  const activeFile = useMemo(
    () => files.find((file) => file.file === selected) ?? files[0],
    [files, selected],
  );

  return (
    <Box>
      {files.length > 0 && (
        <Tabs
          value={activeFile?.file ?? false}
          onChange={(_, value: string) => setSelected(value)}
          sx={{
            minHeight: 36,
            borderBottom: "1px solid var(--border-primary)",
            // Cap the strip height; when tabs overflow they wrap onto
            // additional rows and the scroller scrolls vertically.
            "& .MuiTabs-scroller": {
              maxHeight: 36 * 3,
              overflowX: "hidden !important",
              overflowY: "auto !important",
            },
            "& .MuiTabs-flexContainer": {
              flexWrap: "wrap",
            },
            // Hide the sliding indicator, which mispositions once tabs wrap.
            "& .MuiTabs-indicator": {
              display: "none",
            },
            "& .MuiTab-root": {
              color: "var(--color-primary)",
              minHeight: 36,
              textTransform: "none",
            },
          }}
        >
          {files.map((file) => (
            <Tab
              key={file.file}
              value={file.file}
              label={fileLabel(file.file)}
            />
          ))}
        </Tabs>
      )}
      <PlayGround code={activeFile?.result ?? ""} language={language} />
    </Box>
  );
}
