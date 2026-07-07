import os from "node:os";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { InstallError } from "./errors.js";
import { uniqueValues } from "../utils/collections.js";
export const AGENTS = [
    "codex",
    "claude",
    "portable",
    "vscode",
    "copilot",
    "amp",
    "goose",
    "opencode",
    "factory",
    "cursor",
];
export const SCOPES = ["repo", "user", "cwd"];
export function resolveRepoRoot(cwd) {
    try {
        const out = execFileSync("git", ["rev-parse", "--show-toplevel"], {
            cwd,
            encoding: "utf8",
        }).trim();
        if (out)
            return out;
    }
    catch {
        // fall through
    }
    return cwd;
}
function configRoot() {
    return process.env.XDG_CONFIG_HOME ?? path.join(os.homedir(), ".config");
}
const scopeResolversByAgent = {
    codex: {
        repo: (cwd) => path.join(resolveRepoRoot(cwd), ".codex/skills"),
        cwd: (cwd) => path.join(cwd, ".codex/skills"),
        user: () => {
            const root = process.env.CODEX_HOME ?? path.join(os.homedir(), ".codex");
            return path.join(root, "skills");
        },
    },
    claude: {
        repo: (cwd) => path.join(resolveRepoRoot(cwd), ".claude/skills"),
        user: () => path.join(os.homedir(), ".claude/skills"),
    },
    portable: {
        repo: (cwd) => path.join(resolveRepoRoot(cwd), ".agents/skills"),
        user: () => path.join(configRoot(), "agents/skills"),
    },
    vscode: {
        repo: (cwd) => path.join(resolveRepoRoot(cwd), ".github/skills"),
    },
    copilot: {
        repo: (cwd) => path.join(resolveRepoRoot(cwd), ".github/skills"),
    },
    amp: {
        repo: (cwd) => path.join(resolveRepoRoot(cwd), ".agents/skills"),
        user: () => path.join(configRoot(), "agents/skills"),
    },
    goose: {
        repo: (cwd) => path.join(resolveRepoRoot(cwd), ".agents/skills"),
        user: () => path.join(configRoot(), "agents/skills"),
    },
    opencode: {
        repo: (cwd) => path.join(resolveRepoRoot(cwd), ".opencode/skill"),
        user: () => path.join(configRoot(), "opencode/skill"),
    },
    factory: {
        repo: (cwd) => path.join(resolveRepoRoot(cwd), ".factory/skills"),
        user: () => path.join(os.homedir(), ".factory/skills"),
    },
    cursor: {
        repo: (cwd) => path.join(resolveRepoRoot(cwd), ".cursor/skills"),
    },
};
export function assertAgent(value) {
    if (AGENTS.includes(value)) {
        return value;
    }
    throw new InstallError(`Unsupported agent: ${value}`);
}
export function assertScope(value) {
    if (SCOPES.includes(value)) {
        return value;
    }
    throw new InstallError(`Unsupported scope: ${value}`);
}
export function supportedScopesForAgent(agent) {
    return Object.keys(scopeResolversByAgent[agent]);
}
export function sharedScopesForAgents(agents) {
    const uniqueAgents = uniqueValues(agents);
    if (uniqueAgents.length === 0) {
        return [];
    }
    const first = uniqueAgents[0];
    return supportedScopesForAgent(first).filter((scope) => uniqueAgents.every((agent) => supportedScopesForAgent(agent).includes(scope)));
}
export function assertSupportedAgentScopes(agents, scopes) {
    for (const agent of uniqueValues(agents)) {
        const supported = supportedScopesForAgent(agent);
        for (const scope of uniqueValues(scopes)) {
            if (!supported.includes(scope)) {
                throw new InstallError(`Unsupported agent/scope: ${agent} ${scope}`);
            }
        }
    }
}
export function resolveSkillsRoot(agent, scope, cwd) {
    const resolver = scopeResolversByAgent[agent][scope];
    if (!resolver) {
        throw new InstallError(`Unsupported agent/scope: ${agent} ${scope}`);
    }
    return resolver(cwd);
}
//# sourceMappingURL=resolve.js.map