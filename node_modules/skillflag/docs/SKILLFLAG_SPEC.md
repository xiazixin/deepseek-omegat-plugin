---
title: Skillflag Specification
author: Onur Solmaz <2453968+osolmaz@users.noreply.github.com>
date: 2026-01-11
---

# Skillflag Specification

Version: 0.1 (draft)
Status: Informational + Normative (uses **MUST / SHOULD / MAY**)

## 1. Abstract

**Skillflag** is a CLI convention for exposing “agent skills” (skill directories, not just single markdown files) from a CLI tool via standardized flags, so that:

- the **producing CLI** does _not_ contain agent- or editor-specific installation logic, and
- a separate **installer/adaptor CLI** (or simple shell redirection) can install a chosen skill into a chosen agent tool and scope.

Skillflag defines two primary operations:

- **Discovery**: `--skill list`
- **Export**: `--skill export <id>` (exports the full skill directory as a tar stream on stdout)

## 2. Motivation

Skillflag is designed around these constraints:

1. **OS-independent distribution**
   CLIs may be installed via language ecosystems (npm, cargo, pip, etc.) and run cross-platform. Filesystem conventions like global manpage directories are not a reliable assumption.

2. **Skills are more than a markdown file**
   A skill often includes scripts, templates, schemas, examples, test fixtures, or other assets that must be shipped as a directory bundle.

3. **Avoid per-agent installation logic in producer CLIs**
   It is undesirable for every CLI to learn how to install into every agent tool (Claude Code, Codex, Amp, etc.) and every scope (repo/user/system). That mapping belongs in a separate adaptor installer.

4. **Bundling skills with the CLI package is simpler than maintaining a separate registry**
   Publishing skills alongside the CLI’s normal distribution mechanism reduces operational overhead and version skew.

5. **Users want selective installation**
   Installing a CLI should not implicitly install all of its skills into all local agent tools. Users should be able to list available skills and install only what they want, where they want.

## 3. Goals and non-goals

### 3.1 Goals

- Provide a **minimal**, **portable**, **shell-friendly** interface:
  - list skills
  - export a skill directory

- Keep producer CLIs **tool-agnostic** (no knowledge of target agent install paths).
- Enable both interactive use (humans) and automation (scripts/CI).

### 3.2 Non-goals

- Defining how agent tools internally discover, load, or activate skills at runtime.
- Defining a central skill registry, marketplace, signing infrastructure, or dependency installation mechanism.
- Defining how installers resolve conflicts, pin versions, or manage lockfiles (those can exist, but are outside the core Skillflag interface).

Note: Skillflag does define where installers place skill files (see skill-install companion spec). Runtime discovery and activation by the agent tool itself is out of scope.

## 4. Terminology

- **Producer CLI**: The tool that _bundles_ skills and implements the Skillflag interface (e.g., `mycli`).
- **Skill**: A directory containing `SKILL.md` and optional additional files (scripts/assets/etc.).
- **Skill ID**: The identifier used with `--skill <id>`.
- **Exporter**: The part of the producer CLI that emits the skill bundle.
- **Installer/Adaptor CLI**: A separate tool that consumes a skill directory (or tar stream) and installs it into a specific agent tool and scope.

## 5. Required CLI flags

A Skillflag-compliant producer CLI **MUST** implement:

1. `--skill list`
2. `--skill export <id>`

A producer CLI **MAY** additionally implement:

- `--skill list --json`
- `--skill show <id>` (print skill documentation)

Skillflag does **not** require any particular command substructure (`tool skills ...`) because the goal is a "`--help`-class" universal convention based on flags.

## 6. Optional convenience: `--skill install [<id>]`

A Skillflag-compliant producer CLI **MAY** implement `--skill install [<id>]` as a convenience that combines export + install in a single step.

Producers **MUST NOT** implement agent-specific path resolution.

The `--skill install` convenience **MUST** delegate installation to a Skillflag-compliant installer (e.g. `skill-install`). Producers are responsible for skill discovery and export; installers are responsible for agent-specific placement.

When invoked without required arguments (`agent`, `scope`) and stdin is a TTY, implementations **SHOULD** offer an interactive selection flow.

The interactive behavior, prompts, defaults, and UX are implementation-defined.

When invoked with all required arguments, it **MUST** behave equivalently to:

```bash
tool --skill export <id> | skill-install --agent <agent> --scope <scope>
```

## 7. Discovery: `--skill list`

### 7.1 Behavior

- `tool --skill list` **MUST** print the list of available Skill IDs to **stdout**.
- Output **MUST NOT** include banners, progress text, or other non-data content on stdout.
- Diagnostics and errors **MUST** go to **stderr**.

### 7.2 Output format (text)

- Each skill **MUST** appear on a single line.
- The line **MUST** begin with the `Skill ID`.
- A short summary **MAY** follow, separated by a single tab (`\t`).

Recommended format:

```
<id>\t<summary>
```

If summaries are included:

- `<summary>` **MUST NOT** contain newlines or tabs.

### 7.3 Ordering

- Output ordering **SHOULD** be stable and predictable.
- Recommended: sort lexicographically by Skill ID.

### 7.4 Optional JSON mode

If `tool --skill list --json` is provided:

- It **MUST** print a single JSON object to stdout.
- It **MUST NOT** print additional text to stdout.

Schema:

```json
{
  "skillflag_version": "0.1",
  "skills": [
    {
      "id": "tmux",
      "summary": "Drive an interactive tmux session",
      "version": "1.0.0",
      "files": 3,
      "digest": "sha256:a1b2c3..."
    }
  ]
}
```

Field requirements:

- `id` (string, **required**): Skill identifier. MUST NOT be empty.
- `summary` (string, optional): If omitted, treat as empty string. If present, MUST be a string (not null).
- `version` (string, optional): Semver-style version. If omitted, assume unversioned. If present, MUST be a non-empty string.
- `files` (integer, optional): Number of files in the skill bundle.
- `digest` (string, **required**): SHA-256 hash of the exported tar stream, prefixed with `sha256:`. MUST be present for integrity verification.

Optional fields MUST be omitted if not provided. Producers MUST NOT emit `null` values. Empty string (`""`) is invalid for `version` and `digest`.

## 8. Viewing: `--skill show <id>` (optional)

If implemented:

- `tool --skill show <id>` **SHOULD** print a human-oriented representation of the skill to stdout.
- Recommended: print `<id>/SKILL.md` content only (no extra banners).

This provides a “manpage-like” experience without OS-specific manpage infrastructure.

## 9. Export: `--skill export <id>`

### 9.1 Behavior

- `tool --skill export <id>` **MUST** write the skill bundle to **stdout** as a tar stream.
- The tar stream **MUST** contain exactly one top-level directory named `<id>/`.
- The directory **MUST** include `<id>/SKILL.md`.
- No additional output is permitted on stdout.

### 9.2 Tar format requirements

To maximize portability, exporters:

- **MUST** produce a tar that can be read by common `tar` implementations.
- **SHOULD** use one of:
  - POSIX ustar, or
  - PAX tar (recommended if long paths are needed)

Exporters **MUST** ensure:

- No absolute paths.
- No `..` path traversal segments.
- All entries are relative under `<id>/`.

### 9.3 Determinism

For reproducible installs and caching:

- Exporters **MUST** emit entries in stable, deterministic order (lexicographic by path recommended).
- Exporters **MUST** normalize metadata to fixed values:
  - `mtime`: `0` (Unix epoch) or a fixed timestamp
  - `uid/gid`: `0`
  - `uname/gname`: empty string or `root`

This ensures identical skill content produces identical tar output and matching digests.

### 9.4 Error handling and exit codes

- Exit `0` on success.
- Exit `1` on any error.
- Write error details to **stderr**.

## 10. Skill directory layout (bundling convention)

Inside the producer CLI’s distribution artifact, skills **SHOULD** be stored under a dedicated resource path:

- `skills/<id>/SKILL.md` (required)
- `skills/<id>/...` (optional additional files)

The producer CLI **MUST** map these bundled resources to the Skillflag interface:

- `--skill list` enumerates available `<id>` directories.
- `--skill export <id>` exports the directory as `<id>/...` in tar form.

This deliberately avoids any assumption about package managers or OS-level install roots.

## 11. Skill ID conventions

Skill IDs **SHOULD** be:

- stable across releases once published
- ASCII lowercase, digits, and `-` / `_` (recommended)
- no spaces

Rationale: IDs appear in shell scripts and filesystem paths.

## 12. Metadata

Skill directories **MUST** conform to the Agent Skills specification (https://agentskills.io/specification). Skillflag does not define additional `SKILL.md` format requirements.

## 13. Security considerations

Skillflag keeps the producer CLI in a “data export” role. That reduces risk, but does not eliminate it.

Recommendations:

- Exporters must prevent path traversal and absolute paths (normative requirement).
- Installers should treat exported bundles as untrusted input:
  - provide `--dry-run` / `--inspect`
  - optionally require explicit opt-in to execute any included scripts

- Bundles may include binaries or scripts; installers should surface that fact clearly.

## 14. Interoperability with a separate installer

Skillflag is designed to compose cleanly with a dedicated installer/adaptor CLI that knows how to install into specific agent tools and scopes.

Expected pipeline shape:

```bash
tool --skill export <id> | skill-install --agent <agent> --scope <scope>
```

The installer is responsible for:

- mapping to agent-specific directories and precedence
- conflict resolution
- optional linking vs copying
- optional lockfiles / version pinning

None of that logic belongs in the producer CLI.

## 15. Examples

### 15.1 List skills

```bash
tool --skill list
```

### 15.2 View the skill documentation (if supported)

```bash
tool --skill show tmux
```

### 15.3 Export and inspect without installing

```bash
tool --skill export tmux | tar -tf -
```

### 15.4 Export and install via an adaptor

```bash
tool --skill export tmux | skill-install --agent codex --scope user
```

### 15.5 Export and manually place somewhere (no adaptor needed)

```bash
mkdir -p .agents/skills/tmux
tool --skill export tmux | tar -x -C .agents/skills
```

(That last example assumes the installer semantics are simply “untar into a skills root”.)

## 16. Conformance checklist

A producer CLI is **Skillflag-compliant** if:

- [ ] `--skill list` outputs Skill IDs on stdout with no extra stdout noise.
- [ ] `--skill list --json` includes `digest` for each skill.
- [ ] `--skill export <id>` emits a tar stream on stdout.
- [ ] (Optional) `--skill install [<id>]` follows the convenience behavior described in section 6.
- [ ] The tar stream contains exactly one top-level directory `<id>/`.
- [ ] `<id>/SKILL.md` exists in the exported stream.
- [ ] Tar entries are deterministic (stable order, normalized metadata).
- [ ] No absolute paths or path traversal segments appear in the tar entries.
- [ ] Failures exit `1` and write errors to stderr.

---

# `skill-install` companion spec (installer side)

Scope: baseline behavior installs one skill bundle into one target agent/tool + scope. Implementations **MAY** additionally support multi-install in a single invocation (multiple sources, agents, and scopes).

### Motivation

- **Skills are directories**, not just `SKILL.md`: they can include scripts, templates, references, assets, etc. (multiple tools describe skills this way). ([OpenAI Developers][1])
- **Producer CLIs should not encode per-agent install logic.** The producer just exposes skill bundles (via Skillflag: `--skill list`, `--skill export <id>`). The installer maps to agent-specific locations.
- **Users must opt in**: installing a CLI must not automatically install all of its skills into every local agent. A single explicit target remains the baseline, but implementations **MAY** offer explicit multi-target invocations.
- **Cross-agent portability exists but paths differ**: several tools already read “portable” directories (notably `.agents/skills` and `~/.config/agents/skills`), while others have native roots like `.claude/skills`, `.codex/skills`, `.github/skills`, etc. ([Block][2])

## 1) Inputs `skill-install` accepts

### 1.1 Directory input

Install from a local skill directory:

- `PATH` **must** be a directory containing `SKILL.md` at its root.
- Implementations **MAY** accept multiple `PATH` values in one command. If supported, each path is treated as an independent source skill bundle.

Example:

```bash
skill-install ./skills/tmux --agent claude --scope repo
```

### 1.2 Stream input (tar on stdin)

Install from a tar stream (e.g., produced by a Skillflag producer’s export):

- If `PATH` is omitted and stdin is not a TTY, `skill-install` **must** read a tar stream from stdin.
- The tar stream **should** contain a single top-level directory `<something>/...` with `SKILL.md` inside that root.

Example:

```bash
producer --skill export tmux | skill-install --agent claude --scope user
```

(Producer-side export format is defined by Skillflag: `--skill export <id>` emits a tar bundle on stdout.)

## 2) CLI surface (minimal, stable)

### 2.1 Synopsis

```bash
skill-install [PATH ...]
  --agent <pi|opencode|codex|claude|portable|vscode|copilot|amp|factory|cursor|goose>
  --scope <repo|user|cwd>
  [--root <path>]
  [--mode <copy|link>]
  [--force]
  [--dry-run]
  [--json]
  [--id <override-skill-id>]
  [--dest <override-destination-root>]
  [--native]        # only for agents where “portable-first” is the default (goose/amp)
  [--legacy]        # only where a legacy target exists (vscode/copilot -> .claude/skills)
```

The reference implementation accepts only one value for `--agent` and one value for `--scope`.
Repeated flags and comma-separated values are rejected.
Multi-target installs remain available through the interactive wizard (multi-select).

### 2.2 Required flags

- `--agent` is **required** unless `--dest` is provided.
- `--scope` is **required** unless `--dest` is provided.

Rationale: avoid silent installs into the wrong agent/tool.

## 3) Skill identification and validation

### 3.1 What `skill-install` must validate

By default, `skill-install` **must** validate:

- `SKILL.md` exists at bundle root.
- `name` and `description` are present in `SKILL.md` metadata (as required by the Agent Skills specification).

### 3.2 Skill ID selection (destination folder name)

Default `skill_id` is the YAML `name`.

- Destination directory name **must** be `skill_id` unless overridden by `--id`.
- If the incoming bundle root directory name differs from `skill_id`, `skill-install` **should** rename on install (and may warn on stderr).

## 4) Repo root resolution (for `--scope repo`)

When `--scope repo` is used:

- If inside a git worktree, `skill-install` **should** use the git repository root as `<project-root>`.
- Otherwise, it **should** use the current working directory as `<project-root>`.
- `--root <path>` overrides detection.

## 5) Install modes and conflict rules

### 5.1 `--mode copy` (default, required)

- Copy the entire skill directory tree to the destination.
- Preserve file contents exactly; preserve execute bits when the platform supports it.
- Should install atomically (write temp dir then rename) to avoid partial installs.

### 5.2 `--mode link` (optional)

- Create a symlink/junction at the destination pointing to the source directory (or to an extracted cache if input was a tar stream).
- If linking is unsupported, fail unless the user explicitly chose a fallback.

### 5.3 Conflicts

If the destination already exists:

- Default behavior: **fail** without modifying anything.
- `--force`: remove and replace.

### 5.4 No code execution

`skill-install` **must not** run any scripts contained in the skill bundle as part of installation.

## 6) Security requirements (tar extraction)

When reading a tar stream, `skill-install` **must**:

- Reject absolute paths.
- Reject `..` traversal.
- Reject special files (device nodes/FIFOs).
- Treat symlinks/hardlinks as unsafe by default:
  - recommended: reject them outright, or ensure they stay within the extracted skill root.

### 6.1 Integrity verification

If the producer provides a digest (via `--skill list --json`), installers **should** verify the tar stream matches the expected `sha256` before extracting. This prevents tampered or corrupted bundles from being installed.

## 7) Destination mapping (what `--agent` + `--scope` means)

The agent list below is maintained by the reference implementation and is not normative. Implementations MAY support additional agents. The `--dest` escape hatch provides forward compatibility for unlisted agents.

This section is intentionally concrete and only covers widely-used tools with documented/observable conventions.

### 7.1 `--agent pi` (Pi / pi-mono)

Pi documents these locations and discovery rules. ([Pi][9])

- `repo` → `<project-root>/.pi/skills/<skill_id>/`
- `user` → `~/.pi/agent/skills/<skill_id>/`

### 7.2 `--agent opencode`

OpenCode documents these locations (plus Claude-compatible ones it also searches). ([OpenCode][6])

- `repo` → `<project-root>/.opencode/skill/<skill_id>/`
- `user` → `${XDG_CONFIG_HOME:-~/.config}/opencode/skill/<skill_id>/`

### 7.3 `--agent codex` (OpenAI Codex CLI / IDE)

Codex documents repo and user-level scopes. ([OpenAI Developers][1])

Default mapping:

- `repo` → `<project-root>/.codex/skills/<skill_id>/`
- `user` → `${CODEX_HOME:-~/.codex}/skills/<skill_id>/`
- `cwd` → `$PWD/.codex/skills/<skill_id>/`

### 7.4 `--agent claude` (Claude Code)

Claude Code documents these locations and precedence. ([Claude Code][3])

- `repo` → `<project-root>/.claude/skills/<skill_id>/`
- `user` → `~/.claude/skills/<skill_id>/`

### 7.5 `--agent portable` (recommended cross-agent target)

Portable roots are explicitly used by Amp and Goose and described as portable across agents in Goose docs. ([Block][2])

- `--scope repo` → `<project-root>/.agents/skills/<skill_id>/`
- `--scope user` → `${XDG_CONFIG_HOME:-~/.config}/agents/skills/<skill_id>/`

### 7.6 `--agent vscode` / `--agent copilot` (GitHub Copilot Agent Skills)

GitHub docs + VS Code docs agree on:

- primary: `.github/skills/`

- legacy supported: `.claude/skills/` ([GitHub Docs][4])

- `repo` → `<project-root>/.github/skills/<skill_id>/`

- `repo --legacy` → `<project-root>/.claude/skills/<skill_id>/`

`user` scope: **unsupported** (docs state repo-level only “currently”). ([GitHub Docs][4])

### 7.7 `--agent amp`

Amp states skills install to `.agents/skills/` by default and also reads `~/.config/agents/skills/` (plus Claude-compatible locations for compatibility). ([AmpCode][5])

Portable-first mapping (default):

- `repo` → `<project-root>/.agents/skills/<skill_id>/`
- `user` → `${XDG_CONFIG_HOME:-~/.config}/agents/skills/<skill_id>/`

### 7.8 `--agent factory` (Factory Droid CLI)

Factory docs specify workspace and personal roots. ([Factory Documentation][7])

- `repo` → `<project-root>/.factory/skills/<skill_id>/`
- `user` → `~/.factory/skills/<skill_id>/`

### 7.9 `--agent cursor` (best-effort; path not confirmed via first-party doc here)

- `repo` → `<project-root>/.cursor/skills/<skill_id>/`
- `user` → unsupported (until confirmed)

If you want to avoid this uncertainty, use `--agent vscode` (for Copilot) or `--agent portable`, which are documented.

### 7.10 `--agent goose`

Goose documents a search order that includes both portable and goose-specific locations (global + project). ([Block][2])

Portable-first mapping (default):

- `repo` → `<project-root>/.agents/skills/<skill_id>/`
- `user` → `${XDG_CONFIG_HOME:-~/.config}/agents/skills/<skill_id>/`

If `--native` is provided:

- `repo` → `<project-root>/.goose/skills/<skill_id>/`
- `user` → `${XDG_CONFIG_HOME:-~/.config}/goose/skills/<skill_id>/` ([Block][2])

## 8) Output conventions

- By default, `skill-install` should print human-readable status to **stderr**.
- With `--json`, print a single JSON object to **stdout**:
  - `agent`, `scope`, `skill_id`, `installed_to`, `mode`, `source` (path or stdin), `replaced` (bool)

## 9) Canonical workflows

### Install a bundled skill from a producer CLI into one agent (repo scope)

```bash
producer --skill export tmux | skill-install --agent claude --scope repo
```

### Install into "portable" so multiple agents can read it

```bash
producer --skill export api-setup | skill-install --agent portable --scope repo
```

### Install from a local directory into Codex user scope

```bash
skill-install ./skills/gh-actions-debug --agent codex --scope user
```

## 10) Escape hatches

### 10.1 Unknown agent/tool

If a tool isn’t listed, `skill-install` must support:

- `--dest <skills-root>` which installs to `<skills-root>/<skill_id>/...`

This keeps the spec future-proof without baking in every new agent.

[1]: https://developers.openai.com/codex/skills/ "Agent Skills"
[2]: https://block.github.io/goose/docs/guides/context-engineering/using-skills/ "Using Skills | goose"
[3]: https://code.claude.com/docs/en/skills "Agent Skills - Claude Code Docs"
[4]: https://docs.github.com/copilot/concepts/agents/about-agent-skills "About Agent Skills - GitHub Docs"
[5]: https://ampcode.com/news/agent-skills "Agent Skills - Amp"
[6]: https://opencode.ai/docs/skills/ "Agent Skills"
[7]: https://docs.factory.ai/cli/configuration/skills "Skills - Factory Documentation"
[8]: https://forum.cursor.com/t/adding-project-rules-becomes-skills-in-2-3-8/147499 "Adding project rules becomes skills in 2.3.8 - Bug Reports - Cursor - Community Forum"
[9]: https://github.com/badlogic/pi-mono/blob/main/packages/coding-agent/docs/skills.md "Skills - pi-mono"
