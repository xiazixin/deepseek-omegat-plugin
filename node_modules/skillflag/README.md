# skillflag

[![npm version](https://img.shields.io/npm/v/skillflag.svg)](https://www.npmjs.com/package/skillflag)
[![npm downloads](https://img.shields.io/npm/dm/skillflag.svg)](https://www.npmjs.com/package/skillflag)
[![CI](https://github.com/dutifuldev/skillflag/actions/workflows/ci.yml/badge.svg)](https://github.com/dutifuldev/skillflag/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Node.js](https://img.shields.io/node/v/skillflag.svg)](https://nodejs.org)

skillflag is a minimal CLI convention for bundling, listing and installing [agent skills](https://agentskills.io), so that you don't have to upload them to separate 3rd party skill registries.

Spec: [Skillflag Specification](docs/SKILLFLAG_SPEC.md)

## Motivation

Think of skillflag as "`--help` or `manpage` for skills": a stable flag-based interface to list and export bundled skills without having to upload it to a third party registry. Any relevant agent instructions live right inside your repo and get published together alongside your tool.

[Agent skills](https://agentskills.io) are self-contained capability packages: a folder with a `SKILL.md` (name, description, instructions) plus any scripts, templates, and references the agent needs to execute a specific workflow.

With skillflag, CLI tools can bundle and list these skills without having to upload it to a skill registry. With `--skill list|show|export`, any agent can discover and install instructions that are required to use the tool.

## Example

Suppose that you have installed a CLI tool to control Philips Hue lights at home, `hue-cli`.

To list the skills that the tool can export, you can run:

```
$ hue-cli --skill list
philips-hue    Control Philips Hue lights in the terminal
```

You can then install it to your preferred coding agent, such as Claude Code:

```
$ hue-cli --skill export philips-hue | npx skillflag install --agent claude
Installed skill philips-hue to .claude/skills/philips-hue
```

You can optionally install the skill to `~/.claude`, to make it global across repos:

```
$ hue-cli --skill export philips-hue | npx skillflag install --agent claude --scope user
Installed skill philips-hue to ~/.claude/skills/philips-hue
```

Even better, once this convention becomes commonplace, agents will by default do all these before they even run the tool, so when you ask it to "install hue-cli", it will know to run `--skill list` the same way a human would run `--help` after downloading a program, and install the necessary skills themselves without being asked to.

## Quick setup — add skillflag to your CLI

Copy the prompt below and paste it into your coding agent. It will add skillflag support to your project.

Currently TypeScript/Node only. [Open an issue](https://github.com/dutifuldev/skillflag/issues) if you'd like support for another language.

```text
Add skillflag to this project so the CLI can bundle and expose agent skills.

1. Install the skillflag library:
   npm install skillflag

2. Create a skill directory at skills/<skill-id>/SKILL.md or
   .agents/skills/<skill-id>/SKILL.md with a YAML frontmatter
   (name, description) and markdown instructions for the agent.

3. In the CLI entrypoint, intercept --skill and delegate to skillflag:

   import { findSkillsRoot, maybeHandleSkillflag } from "skillflag";

   await maybeHandleSkillflag(process.argv, {
     skillsRoot: findSkillsRoot(import.meta.url),
   });

4. Verify it works:
   <tool> --skill list
   <tool> --skill show <id>
   <tool> --skill export <id> | npx skillflag install

5. For the full integration guide:
   https://raw.githubusercontent.com/dutifuldev/skillflag/main/docs/INTEGRATION.md

6. For the skillflag specification:
   https://raw.githubusercontent.com/dutifuldev/skillflag/main/docs/SKILLFLAG_SPEC.md
```

## Install (optional)

```bash
npm install -g skillflag
```

You can also run it without installing by using `npx` (see below).

skillflag is currently implemented in Node/TypeScript. Reach out if you'd like to see it implemented in other languages.

## Quick start

Any CLI that implements the skillflag convention can be used like this:

```bash
# list skills the tool can export
<tool> --skill list
# show a single skill's metadata
<tool> --skill show <id>
# install into Codex user skills
<tool> --skill export <id> | npx skillflag install --agent codex
# install into Claude project skills
<tool> --skill export <id> | npx skillflag install --agent claude --scope repo
```

### Interactive mode

When `--agent` or `--scope` is omitted and a TTY is available, `skill-install` launches an interactive wizard:

```bash
# pipe a skill and let the wizard guide you
<tool> --skill export <id> | npx skillflag install
```

The wizard lets you pick agents and scopes with arrow keys and space to select, then confirms before installing. This works even when stdin is piped.

### Multi-target install

`--agent` and `--scope` flags accept a single value each.
To install to multiple agents/scopes in one run, use the interactive wizard:

```bash
<tool> --skill export <id> | npx skillflag install
```

In the wizard, select multiple entries with space, then confirm the matrix install.

## Add skillflag to your CLI

1. Add the library and ship your skill directory in the package.
2. Add a `skills/<skill-id>/SKILL.md` or
   `.agents/skills/<skill-id>/SKILL.md` in your repo.
3. In your CLI entrypoint, intercept `--skill` and delegate to skillflag.

```bash
npm install skillflag
```

```ts
import { findSkillsRoot, maybeHandleSkillflag } from "skillflag";

await maybeHandleSkillflag(process.argv, {
  skillsRoot: findSkillsRoot(import.meta.url),
});
```

See the full guide in [docs/INTEGRATION.md](docs/INTEGRATION.md).

## Supported agents

codex, claude, portable, vscode, copilot, amp, goose, opencode, factory, cursor

## Supported scopes

| Scope  | Description                                                   |
| ------ | ------------------------------------------------------------- |
| `repo` | Install into the current repository (e.g. `.codex/skills/`)   |
| `user` | Install into the user's home config (e.g. `~/.codex/skills/`) |
| `cwd`  | Install relative to the current working directory             |

## Help

```bash
skill-install --help
```

## Bundled skill

This repo ships a single bundled skill at `skills/skillflag/` that documents both `skillflag` and `skill-install`.
