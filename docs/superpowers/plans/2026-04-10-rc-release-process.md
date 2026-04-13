# RC Release Process Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Automatically publish RC-versioned artifacts when a GitHub Release is created from a non-master branch.

**Architecture:** The version action gains a `branch` input and RC-number auto-detection via git tag scanning. The workflow passes the branch and conditionally uses `--tag rc` for npm. All other jobs consume the version via the existing `VERSION` env var unchanged.

**Tech Stack:** GitHub Actions (composite action, workflow YAML), bash, git

---

### Task 1: Update version action to support RC detection

**Files:**
- Modify: `.github/actions/version/action.yml` (full rewrite of the file)

- [ ] **Step 1: Rewrite the version action**

Replace the entire contents of `.github/actions/version/action.yml` with:

```yaml
name: 'Version number'
description: 'Determine the publish version — stable from master, RC from other branches'
inputs:
  version:
    description: 'Git tag (e.g. v1.0.0)'
    required: true
  branch:
    description: 'Branch the release targets (e.g. master, feature/xyz)'
    required: true
outputs:
  version:
    description: 'Resolved version number (e.g. 1.0.0 or 1.0.0-RC.1)'
    value: ${{ steps.version.outputs.version }}
runs:
  using: 'composite'
  steps:
    - id: version
      name: Resolve version number
      shell: bash
      run: |
        TAG="${{ inputs.version }}"
        BRANCH="${{ inputs.branch }}"

        # Validate tag format: v{major}.{minor}.{patch}
        if [[ ! "$TAG" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
          echo "::error::Tag format invalid: $TAG (expected v{major}.{minor}.{patch})"
          exit 1
        fi

        # Strip the v prefix
        BASE_VERSION="${TAG:1}"

        if [[ "$BRANCH" == "master" ]]; then
          echo "Stable release: $BASE_VERSION"
          echo "version=$BASE_VERSION" >> $GITHUB_OUTPUT
        else
          # Find the highest existing RC number for this base version
          HIGHEST_RC=0
          for t in $(git tag --list "v${BASE_VERSION}-RC.*"); do
            # Extract the RC number after the last dot
            RC_NUM="${t##*.}"
            if [[ "$RC_NUM" =~ ^[0-9]+$ ]] && (( RC_NUM > HIGHEST_RC )); then
              HIGHEST_RC=$RC_NUM
            fi
          done

          NEXT_RC=$(( HIGHEST_RC + 1 ))
          RC_VERSION="${BASE_VERSION}-RC.${NEXT_RC}"

          echo "RC release: $RC_VERSION (branch: $BRANCH)"
          echo "version=$RC_VERSION" >> $GITHUB_OUTPUT

          # Create a traceability tag
          git tag "v${RC_VERSION}"
          git push origin "v${RC_VERSION}"
        fi
```

- [ ] **Step 2: Verify the action syntax is valid**

Run:
```bash
python3 -c "import yaml; yaml.safe_load(open('.github/actions/version/action.yml'))" && echo "YAML valid"
```
Expected: `YAML valid`

- [ ] **Step 3: Commit**

```bash
git add .github/actions/version/action.yml
git commit -m "feat: add RC version detection to version action

When branch is not master, automatically resolves the next RC number
by scanning existing git tags (e.g. 1.0.0-RC.1, 1.0.0-RC.2)."
```

---

### Task 2: Pass branch to version action in workflow

**Files:**
- Modify: `.github/workflows/build.yml` (version job, ~line 369-375)

- [ ] **Step 1: Update the version job checkout to fetch all tags**

In `.github/workflows/build.yml`, find the version job's checkout step (line 370):

```yaml
      - uses: actions/checkout@v4
```

Replace with:

```yaml
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
```

This is needed so `git tag --list` in the version action can see all existing tags.

- [ ] **Step 2: Pass the branch to the version action**

In the same version job, find the version step (lines 371-375):

```yaml
      - id: version
        name: Generate version
        uses: ./.github/actions/version
        with:
          version: ${{ github.event.release.tag_name }}
```

Replace with:

```yaml
      - id: version
        name: Generate version
        uses: ./.github/actions/version
        with:
          version: ${{ github.event.release.tag_name }}
          branch: ${{ github.event.release.target_commitish }}
```

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "feat: pass branch to version action for RC detection"
```

---

### Task 3: Add RC dist-tag for npm publishing

**Files:**
- Modify: `.github/workflows/build.yml` (release-lib-npm job, ~line 579-582)

- [ ] **Step 1: Update the npm publish step to use `--tag rc` for RC versions**

In `.github/workflows/build.yml`, find the npm Publish step (lines 579-582):

```yaml
      - name: Publish
        working-directory: ./src/plugin/npm/build/dist/js/productionLibrary
        run: |
          npm publish --access public
```

Replace with:

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

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "feat: publish npm RC versions with 'rc' dist-tag"
```
