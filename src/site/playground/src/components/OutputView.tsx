import { ReactNode, useMemo } from "react";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { Box } from "@mui/material";
import { WsEmitted } from "@flock/wirespec";
import { PlayGround } from "./PlayGround";
import { Breadcrumb } from "./Breadcrumb";
import { FileTree } from "./FileTree";
import { Language } from "../routes";
import { foldRawConversions } from "../utils/FoldRawConversions";
import { counterpartOf, groupFiles } from "../utils/FileGroups";

interface OutputViewProps {
  files: WsEmitted[];
  language: Language;
  controls?: ReactNode;
}

export function OutputView({ files, language, controls }: OutputViewProps) {
  const navigate = useNavigate({ from: "/" });
  const { file: selected, tree: treeOpen = false } = useSearch({ from: "/" });
  const setSelected = (file: string) =>
    navigate({ replace: true, search: (prev) => ({ ...prev, file }) });

  const { prefix, groups } = useMemo(() => groupFiles(files), [files]);
  // Resolve the selected file by path so the selection survives recompilation
  // (a fresh `files` array on every edit) and switching languages (other
  // paths). When it has no counterpart, fall back to the first file.
  const activeFile = useMemo(
    () => counterpartOf(groups, selected) ?? files[0],
    [groups, files, selected],
  );

  return (
    // The width the editor has on its own, so the breadcrumb shrinks with it
    // and the tree and the editor share it. The divider counts towards it, or
    // the page would be a pixel wider than the viewport.
    <Box
      width={{ xs: "100vw", sm: "50vw" }}
      boxSizing="border-box"
      borderLeft={{ sm: "1px solid var(--border-primary)" }}
    >
      <Breadcrumb
        prefix={prefix}
        groups={groups}
        active={activeFile?.file}
        onSelect={setSelected}
        treeOpen={treeOpen}
        onToggleTree={() =>
          navigate({
            replace: true,
            search: (prev) => ({ ...prev, tree: !prev.tree || undefined }),
          })
        }
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
