# Automatic RC Release Process

## Summary

When a GitHub Release is created from a non-master branch, the release pipeline automatically publishes all artifacts with an RC (release candidate) version suffix. Releases from `master` continue to publish stable versions as today.

## Versioning Scheme

- **Stable**: tag `v1.0.0` on `master` -> publishes version `1.0.0`
- **RC**: tag `v1.0.0` on any other branch -> publishes version `1.0.0-RC.{n}`

The RC number `{n}` starts at 1 and increments automatically. The workflow determines the next RC number by scanning existing git tags matching `v{version}-RC.*`.

Examples:
- First RC for `1.0.0` -> `1.0.0-RC.1`
- Second RC for `1.0.0` -> `1.0.0-RC.2`
- Tag `v1.0.0` on `master` -> `1.0.0` (stable, unchanged behavior)

## Publishing Behavior

### Maven Central

No changes to the publishing mechanism. `1.0.0-RC.1` is a valid Maven version. Maven version ordering treats RC versions as older than the stable release: `1.0.0-RC.1 < 1.0.0-RC.2 < 1.0.0`. Users must explicitly specify an RC version in their dependency declaration to use it.

### npm

RC versions are published with the `rc` dist-tag to prevent them from becoming the `latest` version:

- Stable: `npm publish --access public` (tagged as `latest` by default)
- RC: `npm publish --access public --tag rc`

Users install RCs explicitly: `npm install @flock/wirespec@rc`

### Other artifacts

CLI binaries, IDE plugins, and docs are published with the RC version naturally through the existing `VERSION` env var mechanism.

## Changes Required

### 1. `.github/actions/version/action.yml`

Add a new input `branch` (the branch the release was created from). Modify the logic:

- Accept the tag format `v{major}.{minor}.{patch}` (unchanged)
- If `branch` is `master`: output the version as-is (current behavior)
- If `branch` is not `master`:
  - Scan existing git tags matching `v{version}-RC.*`
  - Determine the highest existing RC number (0 if none exist)
  - Output `{version}-RC.{n+1}`
  - Create a new git tag `v{version}-RC.{n+1}` for traceability

### 2. `.github/workflows/build.yml`

**Version job**: pass the branch name to the version action:

```yaml
- id: version
  uses: ./.github/actions/version
  with:
    version: ${{ github.event.release.tag_name }}
    branch: ${{ github.event.release.target_commitish }}
```

**`release-lib-npm` job**: conditionally add `--tag rc`:

```yaml
- name: Publish
  working-directory: ./src/plugin/npm/build/dist/js/productionLibrary
  run: |
    if [[ "$VERSION" == *"-RC."* ]]; then
      npm publish --access public --tag rc
    else
      npm publish --access public
    fi
```

All other release jobs receive the version through the `VERSION` env var and require no changes.

## Edge Cases

| Scenario | Result |
|----------|--------|
| Tag `v1.0.0` on `master` | Publishes `1.0.0` (stable) |
| Tag `v1.0.0` on `ir` (first time) | Publishes `1.0.0-RC.1` |
| Tag `v1.0.0` on `ir` (second time) | Publishes `1.0.0-RC.2` |
| Tag `v1.0.0` on `feature/xyz` | Publishes `1.0.0-RC.3` (continues from highest existing RC) |
| Tag `v2.0.0` on `ir` | Publishes `2.0.0-RC.1` (new base version, starts at 1) |
| Stable `1.0.0` already published, then RC tagged | Publishes `1.0.0-RC.{n}` — valid but unusual; no harm done |

## What Does Not Change

- GPG signing and Sonatype staging configuration
- Gradle build and convention plugins
- The `VERSION` env var mechanism used by all modules
- IntelliJ and VSCode plugin publishing mechanics
- CLI binary build process
- Docs and playground deployment
