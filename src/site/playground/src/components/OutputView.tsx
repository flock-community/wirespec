import { ReactNode, useMemo, useState } from "react";
import { Box } from "@mui/material";
import { WsEmitted } from "@flock/wirespec";
import { PlayGround } from "./PlayGround";
import { Breadcrumb } from "./Breadcrumb";
import { FileTree } from "./FileTree";
import { Language } from "../routes";
import { foldRawConversions } from "../utils/FoldRawConversions";
import { groupFiles } from "../utils/FileGroups";

interface OutputViewProps {
  files: WsEmitted[];
  language: Language;
  controls?: ReactNode;
}

export function OutputView({ files, language, controls }: OutputViewProps) {
  const [selected, setSelected] = useState<string>();
  const [treeOpen, setTreeOpen] = useState(false);

  // Resolve the currently selected file by path so the selection survives
  // recompilation (which produces a fresh `files` array on every edit). When
  // the selected file no longer exists, fall back to the first file.
  const activeFile = useMemo(
    () => files.find((file) => file.file === selected) ?? files[0],
    [files, selected],
  );
  const { prefix, groups } = useMemo(() => groupFiles(files), [files]);

  return (
    // The width the editor has on its own, so the breadcrumb shrinks with it
    // and the tree and the editor share it.
    <Box width={{ xs: "100vw", sm: "50vw" }}>
      <Breadcrumb
        prefix={prefix}
        groups={groups}
        active={activeFile?.file}
        onSelect={setSelected}
        treeOpen={treeOpen}
        onToggleTree={() => setTreeOpen((open) => !open)}
        controls={controls}
      />
      <Box display="flex">
        {treeOpen && (
          <FileTree
            groups={groups}
            active={activeFile?.file}
            onSelect={setSelected}
          />
        )}
        <Box flex={1} minWidth={0}>
          <PlayGround
            code={activeFile?.result ?? ""}
            language={language}
            onCodeShown={foldRawConversions}
            fill
          />
        </Box>
      </Box>
    </Box>
  );
}
