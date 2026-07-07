---
title: Skillflag Node/TypeScript Implementation Plan
author: Onur Solmaz <2453968+osolmaz@users.noreply.github.com>
date: 2026-01-11
---

## Goal

Implement two deliverables in one repo:

1. A Skillflag-compliant **producer CLI** in Node.js + TypeScript (ESM), following the SimpleDoc stack/workflows, with the minimum required interface:
   - `--skill list`
   - `--skill export <id>`
   - optional: `--skill list --json`, `--skill show <id>`
2. A **skill-install** companion CLI (installer) bundled with its own demo skill so the repository can self-demonstrate end‑to‑end installation.

## Non-goals

- Defining per-agent install paths or any install automation outside the `skill-install` CLI.

## Update (installer included)

- The repo will include `skill-install` as a first‑class deliverable.
- The repo will ship a single bundled skill in `skills/` that documents both `skillflag` and `skill-install`.
- Simpler examples (small, minimal skills) will still be provided for clarity.

## Stack and conventions (mirror SimpleDoc)

- Node.js >= 18, TypeScript, ESM (`"type": "module"`).
- `tsc` builds to `dist/`.
- ESLint + Prettier with zero-warning policy.
- Tests with Node’s built-in `node:test` (SimpleDoc style).

## Proposed project layout

```
skillflag/
  src/
    bin/
      skillflag.ts        # CLI entry
      skill-install.ts    # Installer CLI entry
    core/
      list.ts             # discovery logic
      export.ts           # tar export
      show.ts             # optional
      paths.ts            # skills root + id validation
      tar.ts              # deterministic tar creation
      digest.ts           # sha256 streaming
      errors.ts
    install/
      cli.ts              # installer arg parsing
      extract.ts          # tar validation + extraction
      resolve.ts          # agent/scope destination mapping
      validate.ts         # SKILL.md + frontmatter validation
      copy.ts             # install mode (copy)
      link.ts             # optional mode (link)
      json.ts             # JSON output payload
      errors.ts
  test/
    fixtures/skills/...
    integration/...
  skills/                 # bundled skill covering both skillflag + skill-install
```

## Bundling and distribution (npm)

- Skills live at `skills/<id>/...` with `skills/<id>/SKILL.md` required.
- `package.json` must include `skills` in `files` so the bundle ships with the npm tarball.
- Default runtime lookup uses `skills/` relative to the installed package path (`import.meta.url`).
- `skill-install` will use the bundled skill for end‑to‑end examples.

## Implementation plan

### Scaffolding and tooling

1. Initialize `package.json` similar to SimpleDoc:
   - `build` → `tsc -p tsconfig.json`
   - `lint` → `eslint . --max-warnings 0`
   - `format` / `format:check` → Prettier
   - `test` → `tsc -p tsconfig.test.json` + `node --test`
2. Add `tsconfig.json`, `tsconfig.test.json` mirroring SimpleDoc settings.
3. Add `eslint.config.js` with TypeScript ESLint setup.
4. Add `src/bin/skillflag.ts` as the CLI entry.

### Skill discovery (`--skill list`)

1. Define `skillsRoot` (default: `skills/` at repo/package root).
2. List directories directly under `skills/`.
3. Output only IDs to stdout, one per line, no extra text.
4. Enforce stable lexicographic ordering.
5. Errors go to stderr; exit code `1`.

### Deterministic export (`--skill export <id>`)

1. Validate `<id>` exists and contains `<id>/SKILL.md`.
2. Walk the directory, collect file list, sort lexicographically.
3. Create a tar stream (PAX or ustar) with fixed metadata:
   - `mtime = 0`, `uid/gid = 0`, `uname/gname = ""`
4. Enforce path safety:
   - no absolute paths
   - no `..`
5. Stream tar to stdout, no extra output.

### JSON list (`--skill list --json`)

1. Produce the JSON schema from spec, with `digest` required.
2. Compute `sha256` digest of the deterministic tar stream per skill.
3. Include optional fields (`summary`, `version`, `files`) only when present.
4. Omit `null` fields; empty string invalid for `version` and `digest`.

### Optional `--skill show <id>`

- Print `<id>/SKILL.md` to stdout only.

### Tests

- Fixtures under `test/fixtures/skills/*`.
- Integration tests:
  - list output (text + json)
  - export stream structure (top-level `<id>/` only, contains `SKILL.md`)
  - deterministic output (same hash on repeated export)
  - safety: reject absolute paths / `..`
- Installer tests: tar safety, frontmatter validation, destination mapping, and install modes.

## Acceptance criteria

- `--skill list` prints only IDs (stable order), no stdout noise.
- `--skill export <id>` streams a valid tar with exactly one top-level `<id>/` and `<id>/SKILL.md`.
- Export is deterministic (matching digests).
- All errors go to stderr, exit `1` on failure.
- JSON listing includes `digest` and conforms to the spec.

## Interface stability plan (long-lived, minimal API)

Goal: expose a tiny, framework-agnostic integration surface that can remain unchanged for years. The CLI remains the primary interface; the library API is intentionally minimal and strictly additive over time.

### Public surface (keep tiny)

1. **CLI flags** (spec-defined, stable):
   - `--skill list`
   - `--skill export <id>`
   - optional: `--skill list --json`, `--skill show <id>`

2. **Library entrypoint** (single function):

   ```ts
   export type SkillflagOptions = {
     skillsRoot: URL | string;
     stdout?: NodeJS.WritableStream;
     stderr?: NodeJS.WritableStream;
     now?: Date; // optional for tests
   };

   export async function handleSkillflag(
     argv: string[],
     opts: SkillflagOptions,
   ): Promise<number>;
   ```

   - **No framework types** (no commander/yargs types) to avoid lock-in.
   - **Single function** to avoid surface creep.
   - **Optional deps only** (stdout/stderr injection for tests).

### Integration pattern (stable and safe)

- **Early flag interception** in the CLI entrypoint:
  - detect `--skill` in raw `process.argv`
  - call `handleSkillflag` and return **before** any other CLI code runs
  - avoids stdout noise and side effects

### Compatibility rules (Lindy)

- **Semver + additive changes only**: no breaking changes to CLI flags or `handleSkillflag` signature.
- **Conservative defaults**: `skillsRoot` defaults to `skills/` (same path forever).
- **Behavioral determinism**: tar output order + metadata fixed, so digests remain stable.
- **Strict I/O contract**: stdout is data-only, stderr is diagnostics only.

### Extension strategy (avoid breakage)

- New functionality must be **new flags** or **new optional fields** in JSON.
- Avoid new mandatory options or config files.
- If new behavior is needed, add **new optional parameters** to `SkillflagOptions` with sensible defaults.

### Test contract (pin behavior)

- Snapshot tests for `--skill list` output (text + JSON).
- Golden hash tests for `--skill export` per fixture.
- Strict checks: stdout contains only data, no banners.

## Locked decisions (fully specified)

- **Skills root**: fixed default to `skills/` relative to the installed package. No env/flag overrides in v0.1 (keeps interface stable).
- **JSON fields**: only `id`, `digest`, and `files` are emitted. `summary`/`version` are omitted (frontmatter parsing is deferred).
- **Tar implementation**: use `tar-stream` with fixed `mtime = 0`, `uid/gid = 0`, `uname/gname = \"\"`, lexicographic entry order.
- **Determinism tests**: verify exported tar metadata and ordering in tests (not just structure).
- **Installer included**: `skill-install` is implemented in this repo and ships a single bundled skill covering both tools.

## Examples (include both simple and end-to-end)

- **Bundled skill**: `skills/skillflag/` documents both `skillflag` and `skill-install`.
- **Simple skill**: minimal `skills/hello-world/SKILL.md` for quick demos.
- **End‑to‑end**:
  - `skillflag --skill export skillflag | skill-install --agent codex --scope repo`
  - `skillflag --skill list` and `--skill export <id>` remain the canonical producer examples.
