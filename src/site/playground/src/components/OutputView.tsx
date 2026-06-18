import { useMemo, useState } from "react";
import { Box, Tab, Tabs } from "@mui/material";
import { WsEmitted } from "@flock/wirespec";
import { PlayGround } from "./PlayGround";
import { Language } from "../routes";

interface OutputViewProps {
  files: WsEmitted[];
  allCode: string;
  language: Language;
}

const ALL = "__all__";

export function OutputView({ files, allCode, language }: OutputViewProps) {
  const [selected, setSelected] = useState<string>(ALL);

  // Resolve the currently selected file by name so the selection survives
  // recompilation (which produces a fresh `files` array on every edit). When
  // the selected file no longer exists, fall back to the combined view.
  const activeFile = useMemo(
    () =>
      selected === ALL
        ? undefined
        : files.find((file) => file.typeName === selected),
    [files, selected],
  );

  const tabValue = activeFile ? activeFile.typeName : ALL;
  const code = activeFile ? activeFile.result : allCode;

  return (
    <Box>
      {files.length > 1 && (
        <Tabs
          value={tabValue}
          onChange={(_, value: string) => setSelected(value)}
          variant="scrollable"
          scrollButtons="auto"
          sx={{
            minHeight: 36,
            borderBottom: "1px solid var(--border-primary)",
            "& .MuiTab-root": {
              color: "var(--color-primary)",
              minHeight: 36,
              textTransform: "none",
            },
          }}
        >
          <Tab value={ALL} label="All" />
          {files.map((file) => (
            <Tab
              key={file.typeName}
              value={file.typeName}
              label={file.typeName}
            />
          ))}
        </Tabs>
      )}
      <PlayGround code={code} language={language} />
    </Box>
  );
}
