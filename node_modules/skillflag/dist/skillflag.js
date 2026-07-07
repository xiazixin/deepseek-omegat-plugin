import process from "node:process";
import { SkillflagError, toErrorMessage } from "./core/errors.js";
import { exportSkill } from "./core/export.js";
import { listSkills, listSkillsJson } from "./core/list.js";
import { defaultSkillsRoot, resolveSkillDirFromRoots, resolveSkillsRoot, resolveSkillsRoots, } from "./core/paths.js";
import { showSkill } from "./core/show.js";
import { collectSkillEntries, createTarStream } from "./core/tar.js";
import { uniqueValues } from "./utils/collections.js";
const usageLines = [
    "Usage:",
    "  --skill install [<id> ...] [--agent <agent>] [--scope <scope>] [--force]",
    "  --skill list [--json]",
    "  --skill export <id>",
    "  --skill show <id>",
    "  --skill help",
];
export const SKILLFLAG_HELP_TEXT = [
    "Skillflag help",
    "",
    "Install skillflag globally to get both binaries on your PATH:",
    "  npm install -g skillflag",
    "",
    "Prefer not to install globally? Use npx for one-off runs:",
    "  npx skillflag list",
    "  npx skillflag install --agent codex --scope repo < ./skill.tar",
    "",
    "List available skills:",
    "  tool --skill list",
    "  tool --skill list --json",
    "",
    "Show a skill's documentation:",
    "  tool --skill show <id>",
    "",
    "Export a skill bundle:",
    "  tool --skill export <id>",
    "",
    "Install a skill bundle:",
    "  tool --skill install [<id> ...] [--agent <agent>] [--scope <scope>]",
    "  tool --skill export <id> | skill-install --agent <agent> --scope <scope>",
    "",
    "For full details, read docs/SKILLFLAG_SPEC.md.",
].join("\n");
async function defaultPromptApi() {
    const prompts = await import("@clack/prompts");
    return {
        multiselect: prompts.multiselect,
        isCancel: prompts.isCancel,
    };
}
function resolveSkillActionArgs(argv) {
    const cliArgs = argv.length > 2 ? argv.slice(2) : [...argv];
    const skillIndex = cliArgs.indexOf("--skill");
    if (skillIndex >= 0) {
        return cliArgs.slice(skillIndex + 1);
    }
    return cliArgs;
}
function parseInstallIds(values) {
    const ids = [];
    let index = 0;
    while (index < values.length) {
        const value = values[index];
        if (value.startsWith("-")) {
            break;
        }
        const parsed = value
            .split(",")
            .map((part) => part.trim())
            .filter((part) => part.length > 0);
        ids.push(...parsed);
        index += 1;
    }
    return {
        ids: ids.length > 0 ? uniqueValues(ids) : undefined,
        installArgs: values.slice(index),
    };
}
/**
 * Parse skillflag action arguments from argv.
 *
 * Expected forms:
 * - Node-style argv: `[execPath, scriptPath, ...cliArgs]`
 * - Already-trimmed args: `["--skill", "list"]` or `["list"]`
 *
 * For producer CLIs, parsing starts right after `--skill`.
 * For the standalone `skillflag` binary, parsing starts at `cliArgs[0]`.
 */
function parseSkillArgs(argv) {
    const args = resolveSkillActionArgs(argv);
    const action = args[0];
    if (!action || action.startsWith("-")) {
        throw new SkillflagError(`Missing --skill action.\n${usageLines.join("\n")}`);
    }
    if (action === "install") {
        const rest = args.slice(1);
        const parsed = parseInstallIds(rest);
        return {
            kind: "install",
            ids: parsed.ids,
            installArgs: parsed.installArgs,
        };
    }
    if (action === "list") {
        const json = args.slice(1).includes("--json");
        return { kind: "list", json };
    }
    if (action === "help") {
        return { kind: "help" };
    }
    if (action === "export" || action === "show") {
        const id = args[1];
        if (!id || id.startsWith("-")) {
            throw new SkillflagError(`Missing skill id.\n${usageLines.join("\n")}`);
        }
        return { kind: action, id };
    }
    throw new SkillflagError(`Unknown --skill action: ${action}.\n${usageLines.join("\n")}`);
}
function stdinIsTty(stream) {
    return stream.isTTY === true;
}
async function resolveInstallSkillIds(action, rootDirs, stdin, stdout, promptApi) {
    if (action.ids && action.ids.length > 0) {
        return action.ids;
    }
    const skills = await listSkills(rootDirs);
    if (skills.length === 0) {
        throw new SkillflagError("No skills are available to install.");
    }
    if (skills.length === 1) {
        return [skills[0].id];
    }
    if (!stdinIsTty(stdin)) {
        throw new SkillflagError("Multiple skills are available; pass one or more ids with --skill install <id> [...].");
    }
    const options = skills.map((skill) => ({
        value: skill.id,
        label: skill.id,
        hint: skill.summary,
    }));
    const selected = await promptApi.multiselect({
        message: "Select skills to install",
        options,
        required: true,
        input: stdin,
        output: stdout,
    });
    if (promptApi.isCancel(selected)) {
        throw new SkillflagError("Install cancelled.");
    }
    return uniqueValues(selected);
}
async function runInstallAction(action, rootDirs, opts, stdin, stdout, stderr) {
    const promptApi = opts.promptApi ?? (await defaultPromptApi());
    const skillIds = await resolveInstallSkillIds(action, rootDirs, stdin, stdout, promptApi);
    const inputs = await Promise.all(skillIds.map(async (skillId) => {
        const skillDir = await resolveSkillDirFromRoots(rootDirs, skillId);
        const { entries } = await collectSkillEntries(skillDir, skillId);
        return { kind: "tar", stream: createTarStream(entries) };
    }));
    const { runInstallCli } = await import("./install/cli.js");
    return runInstallCli(["node", "skill-install", ...action.installArgs], {
        stdin: stdin,
        stdout: stdout,
        stderr: stderr,
        cwd: opts.cwd,
        providedInputs: inputs,
        providedSkillIds: skillIds,
    });
}
export async function handleSkillflag(argv, opts) {
    const stdin = opts.stdin ?? process.stdin;
    const stdout = opts.stdout ?? process.stdout;
    const stderr = opts.stderr ?? process.stderr;
    try {
        const action = parseSkillArgs(argv);
        const bundledRoot = resolveSkillsRoot(defaultSkillsRoot());
        const includeBundled = opts.includeBundledSkill !== false;
        const rootDirs = resolveSkillsRoots(includeBundled
            ? [...resolveSkillsRoots(opts.skillsRoot), bundledRoot]
            : opts.skillsRoot);
        if (action.kind === "install") {
            return await runInstallAction(action, rootDirs, opts, stdin, stdout, stderr);
        }
        if (action.kind === "list") {
            if (action.json) {
                const payload = await listSkillsJson(rootDirs);
                stdout.write(JSON.stringify(payload));
            }
            else {
                const skills = await listSkills(rootDirs);
                if (skills.length > 0) {
                    const lines = skills.map((skill) => skill.summary ? `${skill.id}\t${skill.summary}` : skill.id);
                    stdout.write(`${lines.join("\n")}\n`);
                }
            }
            return 0;
        }
        if (action.kind === "export") {
            const skillDir = await resolveSkillDirFromRoots(rootDirs, action.id);
            await exportSkill(skillDir, action.id, stdout);
            return 0;
        }
        if (action.kind === "help") {
            stdout.write(`${SKILLFLAG_HELP_TEXT}\n`);
            return 0;
        }
        const skillDir = await resolveSkillDirFromRoots(rootDirs, action.id);
        await showSkill(skillDir, action.id, stdout);
        return 0;
    }
    catch (err) {
        const message = toErrorMessage(err);
        stderr.write(`${message}\n`);
        return err instanceof SkillflagError ? err.exitCode : 1;
    }
}
export async function maybeHandleSkillflag(argv, opts) {
    if (!argv.includes("--skill")) {
        return false;
    }
    const { exit, ...skillOpts } = opts;
    const exitCode = await handleSkillflag(argv, skillOpts);
    if (exit !== false) {
        const exitFn = exit ?? process.exit;
        exitFn(exitCode);
    }
    return true;
}
//# sourceMappingURL=skillflag.js.map