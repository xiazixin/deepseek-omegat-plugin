---
name: skillflag
description: Skillflag producer/installer usage and install guidance (requires skill-install).
---

# Skillflag + skill-install

This skill documents how to use the Skillflag producer CLI (`skillflag`) and the installer CLI (`skill-install`).

## What this repo provides

- `skillflag --skill list` lists bundled skill IDs
- `skillflag --skill export <id>` exports a skill bundle as a tar stream
- `skill-install` consumes a skill directory or tar stream and installs it into a target agent scope

## Quick start

List skills:

```
skillflag --skill list
```

Export the bundled skill and install into Codex repo scope:

```
skillflag --skill export skillflag | skill-install --agent codex --scope repo
```

Export the bundled skill and install into Claude repo scope:

```
skillflag --skill export skillflag | skill-install --agent claude --scope repo
```

## skill-install inputs

Directory input:

```
skill-install ./skills/skillflag --agent codex --scope repo
```

Tar stream input (from producer):

```
skillflag --skill export skillflag | skill-install --agent codex --scope repo
```

Tar stream input with interactive target selection (when a TTY is available):

```
skillflag --skill export skillflag | skill-install
```

Show installer usage/help:

```
skill-install --help
```

## Notes

- `skillflag` is the producer interface; it never installs.
- `skill-install` validates `SKILL.md` frontmatter and installs by copy.
- The full specification lives at `docs/SKILLFLAG_SPEC.md`.

## Spec essentials (producer)

- Producer CLIs MUST implement:
  - `--skill list`
  - `--skill export <id>`
- `--skill list --json` MAY be provided and must emit a single JSON object to stdout.
- `--skill list` writes only skill IDs to stdout (no banners).
- `--skill export <id>` streams a tar with a single top‑level `<id>/` and `<id>/SKILL.md`.
- Export output MUST be deterministic: sorted entries, fixed `mtime = 0`, `uid/gid = 0`.
- All errors go to stderr, exit code `1` on failure.
- Skills should be bundled under `skills/<id>/SKILL.md` or `.agents/skills/<id>/SKILL.md`.

## Adding Skillflag to a Node CLI (library integration)

Install the library:

```
npm install skillflag
```

Global install (exposes `skillflag` and `skill-install` on PATH):

```
npm install -g skillflag
```

Run without installing (uses bundled binaries):

```
npx skillflag --skill list
npx skillflag install --agent codex --scope repo ./skills/skillflag
```

Early‑intercept in your CLI entrypoint so stdout stays clean:

```ts
// src/bin/cli.ts
import { findSkillsRoot, maybeHandleSkillflag } from "skillflag";

await maybeHandleSkillflag(process.argv, {
  skillsRoot: findSkillsRoot(import.meta.url),
  // includeBundledSkill: false, // opt out of the built-in skillflag skill
});

// ... normal CLI startup here
```

Repository layout (bundled skills):

```
skills/
  my-skill/
    SKILL.md
    templates/...
```

Portable agent-skill layout is also supported:

```
.agents/
  skills/
    my-skill/
      SKILL.md
      templates/...
```

Package distribution:

- Ensure `skills/` or `.agents/skills/` is included in `package.json` `files` so it ships with the npm tarball.

## Installing without global install

Use the `skillflag install` convenience command to avoid a global install:

```
npx skillflag install --agent codex --scope repo ./skills/skillflag
```
