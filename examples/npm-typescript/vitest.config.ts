import { defineConfig } from "vitest/config";

export default defineConfig({
  // @flock/wirespec is linked as a local `file:` dependency (a symlink to the built
  // package), and declares `msw` as an optional peer dependency that lives in this
  // project's node_modules. Inlining the package makes Vitest transform it (instead of
  // letting Node load it natively from the real, dependency-less dist path), and
  // preserveSymlinks resolves `import 'msw'` through the symlink so it is found here.
  resolve: {
    preserveSymlinks: true,
  },
  test: {
    server: {
      deps: {
        inline: ["@flock/wirespec"],
      },
    },
  },
});
