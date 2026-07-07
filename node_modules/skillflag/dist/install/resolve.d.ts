export declare const AGENTS: readonly ["codex", "claude", "portable", "vscode", "copilot", "amp", "goose", "opencode", "factory", "cursor"];
export declare const SCOPES: readonly ["repo", "user", "cwd"];
export type Agent = (typeof AGENTS)[number];
export type Scope = (typeof SCOPES)[number];
export declare function resolveRepoRoot(cwd: string): string;
export declare function assertAgent(value: string): Agent;
export declare function assertScope(value: string): Scope;
export declare function supportedScopesForAgent(agent: Agent): Scope[];
export declare function sharedScopesForAgents(agents: Agent[]): Scope[];
export declare function assertSupportedAgentScopes(agents: Agent[], scopes: Scope[]): void;
export declare function resolveSkillsRoot(agent: Agent, scope: Scope, cwd: string): string;
//# sourceMappingURL=resolve.d.ts.map