import { spawnSync } from "node:child_process";
import { mkdtempSync, readFileSync, readdirSync, rmSync } from "node:fs";
import { createRequire } from "node:module";
import { tmpdir } from "node:os";
import path from "node:path";

/*
 * Fills code blocks marked with `wirespec` with the output of the Wirespec CLI, so the docs never contain
 * hand-copied generated code. The page declares what to compile in its front matter:
 *
 *   wirespec:
 *     source: todo.ws          # relative to the page
 *     language: Kotlin         # a --language of the CLI
 *     package: community.flock.todo   # optional
 *     shared: true             # optional, also emit the shared Wirespec file
 *     ignoreDefaults: true     # optional
 *
 * A code block picks what to show, and may override any of the settings above:
 *
 *   ```wirespec wirespec source               the Wirespec source itself
 *   ```shell wirespec command                 the CLI command that generates the output
 *   ```text wirespec tree                     the generated files
 *   ```kotlin wirespec file=model/Todo.kt     one generated file, matched on the end of its path
 *   ```json wirespec select=components.schemas.Todo   part of a generated JSON file
 *   ```text wirespec error                    the error the compiler reports
 */

type Settings = {
  source?: string;
  language?: string;
  package?: string;
  shared?: boolean;
  ignoreDefaults?: boolean;
};

type Code = { type: "code"; lang?: string; meta?: string; value: string };
type Node = { type: string; children?: Node[] } & Partial<Code>;
type File = { path: string; data: { frontMatter?: { wirespec?: Settings } } };

type Compilation = { files: Map<string, string>; errors: string };

const cli = path.join(
  path.dirname(createRequire(path.join(process.cwd(), "package.json")).resolve("@flock/wirespec")),
  "wirespec-bin.mjs",
);
const compilations = new Map<string, Compilation>();

const parseMeta = (meta: string): Map<string, string> =>
  new Map(
    [...meta.matchAll(/([\w-]+)(?:=("[^"]*"|\S+))?/g)].map(([, key, value]) => [
      key,
      value?.replace(/^"(.*)"$/, "$1") ?? "true",
    ]),
  );

const settingsOf = (meta: Map<string, string>, page: Settings): Settings => ({
  ...page,
  ...(meta.has("language") && { language: meta.get("language") }),
  ...(meta.has("package") && { package: meta.get("package") }),
  ...(meta.has("shared") && { shared: meta.get("shared") === "true" }),
  ...(meta.has("ignoreDefaults") && { ignoreDefaults: meta.get("ignoreDefaults") === "true" }),
});

const argumentsOf = (source: string, output: string, settings: Settings): string[] => [
  "compile",
  "-i",
  source,
  "-o",
  output,
  "-l",
  settings.language ?? "",
  ...(settings.package ? ["-p", settings.package] : []),
  ...(settings.shared ? ["--shared"] : []),
  ...(settings.ignoreDefaults ? ["--ignore-defaults"] : []),
];

const filesIn = (dir: string, root = dir): [string, string][] =>
  readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(dir, entry.name);
    return entry.isDirectory()
      ? filesIn(full, root)
      : [[path.relative(root, full).split(path.sep).join("/"), readFileSync(full, "utf8")] as [string, string]];
  });

const compile = (source: string, settings: Settings): Compilation => {
  const key = JSON.stringify([source, settings]);
  if (!compilations.has(key)) {
    const out = mkdtempSync(path.join(tmpdir(), "wirespec-docs-"));
    try {
      const args = [cli, ...argumentsOf(source, out, settings), "--log-level", "ERROR"];
      const result = spawnSync(process.execPath, args, { encoding: "utf8" });
      if (result.error) throw result.error;
      compilations.set(key, { files: new Map(filesIn(out)), errors: result.stderr.trim() });
    } finally {
      rmSync(out, { recursive: true, force: true });
    }
  }
  return compilations.get(key) as Compilation;
};

const succeeded = (compilation: Compilation, where: string): Map<string, string> => {
  if (compilation.errors) throw new Error(`${where}: Wirespec failed to compile:\n${compilation.errors}`);
  return compilation.files;
};

const pick = (files: Map<string, string>, file: string | undefined, where: string): [string, string] => {
  const matches = [...files].filter(([name]) => !file || name === file || name.endsWith(`/${file}`));
  if (matches.length !== 1) {
    throw new Error(
      `${where}: ${matches.length} generated files match '${file ?? ""}', expected one of:\n${[...files.keys()].join("\n")}`,
    );
  }
  return matches[0];
};

const inline = (value: unknown): string =>
  Array.isArray(value)
    ? `[${value.map(inline).join(", ")}]`
    : value !== null && typeof value === "object"
      ? Object.keys(value).length === 0
        ? "{}"
        : `{ ${Object.entries(value).map(([k, v]) => `${JSON.stringify(k)}: ${inline(v)}`).join(", ")} }`
      : JSON.stringify(value);

// Pretty prints JSON, but keeps every object or array that fits on one line on one line.
const formatJson = (value: unknown, level = 0, column = 0): string => {
  const flat = inline(value);
  if (column + flat.length <= 76 || value === null || typeof value !== "object") return flat;
  const pad = "  ".repeat(level + 1);
  const close = "  ".repeat(level);
  const items = Array.isArray(value)
    ? value.map((v) => pad + formatJson(v, level + 1, pad.length))
    : Object.entries(value).map(
        ([k, v]) => `${pad}${JSON.stringify(k)}: ${formatJson(v, level + 1, pad.length + JSON.stringify(k).length + 2)}`,
      );
  const [open, end] = Array.isArray(value) ? ["[", "]"] : ["{", "}"];
  return `${open}\n${items.join(",\n")}\n${close}${end}`;
};

// Follows a dotted path; on an array, a step picks the element with that name, like an Avro schema.
const select = (json: unknown, selector: string, where: string): string => {
  const steps = selector.split(".");
  const value = steps.reduce<unknown>((node, step) => {
    const next = Array.isArray(node)
      ? node.find((element: { name?: string } | null) => element?.name === step)
      : (node as Record<string, unknown> | undefined)?.[step];
    if (next === undefined) throw new Error(`${where}: '${step}' of '${selector}' is not in the generated JSON`);
    return next;
  }, json);
  const last = steps[steps.length - 1];
  return Array.isArray(json) && steps.length === 1
    ? formatJson(value)
    : `${JSON.stringify(last)}: ${formatJson(value, 0, last.length + 4)}`;
};

type Tree = Map<string, Tree | null>;

const treeOf = (paths: string[]): Tree =>
  paths.reduce<Tree>((tree, file) => {
    const parts = file.split("/");
    const dir = parts.slice(0, -1).reduce<Tree>((node, part) => {
      if (!node.has(part)) node.set(part, new Map());
      return node.get(part) as Tree;
    }, tree);
    dir.set(parts[parts.length - 1], null);
    return tree;
  }, new Map());

const renderTree = (tree: Tree, indent = ""): string[] => {
  const entries = [...tree].sort(([a, x], [b, y]) => Number(x !== null) - Number(y !== null) || a.localeCompare(b));
  return entries.flatMap(([name, child], index) => {
    const last = index === entries.length - 1;
    // Collapse directories that only hold one directory, like community/flock/todo.
    let label = name;
    let node = child;
    while (node && node.size === 1 && [...node.values()][0] !== null) {
      const [[next, nested]] = [...node];
      label = `${label}/${next}`;
      node = nested;
    }
    return [
      `${indent}${last ? "└── " : "├── "}${label}`,
      ...(node ? renderTree(node, indent + (last ? "    " : "│   ")) : []),
    ];
  });
};

const render = (code: Code, meta: Map<string, string>, settings: Settings, source: string, where: string): Code => {
  const name = path.basename(source);
  if (meta.has("source")) return { ...code, meta: `title="${name}"`, value: readFileSync(source, "utf8").trimEnd() };
  if (!settings.language) throw new Error(`${where}: no wirespec language in the front matter or the code block`);
  if (meta.has("command")) {
    return { ...code, meta: "", value: ["wirespec", ...argumentsOf(name, "out", settings)].join(" ") };
  }
  const compilation = compile(source, settings);
  if (meta.has("error")) {
    if (!compilation.errors) throw new Error(`${where}: expected Wirespec to fail, but it compiled`);
    return { ...code, meta: "", value: compilation.errors };
  }
  const files = succeeded(compilation, where);
  if (meta.has("tree")) return { ...code, meta: "", value: ["out", ...renderTree(treeOf([...files.keys()]))].join("\n") };
  const [file, content] = pick(files, meta.get("file"), where);
  const selector = meta.get("select");
  if (selector) return { ...code, meta: "", value: select(JSON.parse(content), selector, where) };
  const json = /\.(json|avsc)$/.test(file);
  return { ...code, meta: `title="${file}"`, value: json ? formatJson(JSON.parse(content)) : content.trimEnd() };
};

const transform = (node: Node, file: File): Node => {
  if (node.type === "code" && node.meta) {
    const meta = parseMeta(node.meta);
    if (meta.has("wirespec")) {
      const page = file.data.frontMatter?.wirespec ?? {};
      const settings = settingsOf(meta, page);
      const where = `${file.path} (\`\`\`${node.lang ?? ""} ${node.meta})`;
      if (!settings.source) throw new Error(`${where}: no wirespec source in the front matter`);
      const source = path.resolve(path.dirname(file.path), settings.source);
      return render(node as Code, meta, settings, source, where);
    }
  }
  return node.children ? { ...node, children: node.children.map((child) => transform(child, file)) } : node;
};

export default function wirespec() {
  return (tree: Node, file: File): Node => transform(tree, file);
}
