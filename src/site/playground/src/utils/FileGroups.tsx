import { WsEmitted } from "@flock/wirespec";
import { PACKAGE_NAME } from "../transformations/WirespecToTarget";

const PACKAGE_PATH = PACKAGE_NAME.replace(/\./g, "/");

export type FileGroup = {
  // The package relative to the generated package (e.g. "model"), empty for
  // files directly in it or, for TypeScript, in the root.
  label: string;
  files: WsEmitted[];
};

// "community/flock/wirespec/generated/model/Todo.kt" -> "Todo.kt"
export const fileName = (path: string) => path.split("/").pop() || path;

const packageOf = (path: string) =>
  path.split("/").filter(Boolean).slice(0, -1).join("/");

// Packages outside the generated package, such as the `__init__.py` packages
// Python emits above it, keep their full name.
const labelOf = (pkg: string) =>
  (pkg === PACKAGE_PATH
    ? ""
    : pkg.startsWith(`${PACKAGE_PATH}/`)
      ? pkg.slice(PACKAGE_PATH.length + 1)
      : pkg
  ).replace(/\//g, ".");

// Groups files by package in the order they were emitted, with the root
// files last.
export const groupFiles = (
  files: WsEmitted[],
): { prefix: string; groups: FileGroup[] } => ({
  prefix: files.some((file) => packageOf(file.file).startsWith(PACKAGE_PATH))
    ? PACKAGE_NAME
    : "",
  groups: [...new Set(files.map((file) => packageOf(file.file)))]
    .map((pkg) => ({
      label: labelOf(pkg),
      files: files.filter((file) => packageOf(file.file) === pkg),
    }))
    .sort((a, b) => Number(!a.label) - Number(!b.label)),
});

export const groupOf = (groups: FileGroup[], path?: string) =>
  groups.find((group) => group.files.some((file) => file.file === path));

// "Todo.kt", "Todo.java" and "todo.py" are the same file in another language.
const sameName = (a: string, b: string) => {
  const stem = (path: string) =>
    fileName(path)
      .replace(/\.[^.]*$/, "")
      .replace(/[^a-z0-9]/gi, "")
      .toLowerCase();
  return stem(a) === stem(b);
};

// The file at `path`, or else the same file in the output of another
// language: by name in the same package, by name anywhere, or the first file
// of the same package.
export const counterpartOf = (
  groups: FileGroup[],
  path?: string,
): WsEmitted | undefined => {
  if (!path) return undefined;
  const label = labelOf(packageOf(path));
  const files = groups.flatMap((group) => group.files);
  const group = groups.find((it) => it.label === label);
  return (
    files.find((file) => file.file === path) ??
    group?.files.find((file) => sameName(file.file, path)) ??
    files.find((file) => sameName(file.file, path)) ??
    group?.files[0]
  );
};
