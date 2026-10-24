import { readdirSync, readFileSync } from "node:fs";
import { join, relative } from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("../../../..", import.meta.url));
const skipped = new Set(["node_modules", "build", ".git", ".gradle", ".kotlin"]);

const walk = (dir) =>
  readdirSync(dir, { withFileTypes: true }).flatMap((entry) =>
    skipped.has(entry.name) ? [] : entry.isDirectory() ? walk(join(dir, entry.name)) : [join(dir, entry.name)],
  );

const fences = (path) =>
  [...readFileSync(path, "utf8").matchAll(/^```wirespec[^\n]*\n([\s\S]*?)^```/gm)].map((match, index) => ({
    name: `${relative(root, path)} #${index + 1}`,
    source: match[1],
  }));

/** Every Wirespec source in the repository: the `.ws` files and the ```wirespec fences in the docs. */
export const corpus = () =>
  walk(root).flatMap((path) =>
    path.endsWith(".ws")
      ? [{ name: relative(root, path), source: readFileSync(path, "utf8") }]
      : /\.mdx?$/.test(path)
        ? fences(path)
        : [],
  );
