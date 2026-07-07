import { Readable } from "node:stream";
import { type Agent, type Scope } from "./resolve.js";
export type InstallInput = {
    kind: "dir";
    dir: string;
} | {
    kind: "tar";
    stream: Readable;
};
export type InstallOptions = {
    agent: Agent;
    scope: Scope;
    cwd: string;
    force: boolean;
};
export type InstallResult = {
    skillId: string;
    installedTo: string;
};
export declare function installSkill(input: InstallInput, options: InstallOptions): Promise<InstallResult>;
//# sourceMappingURL=install.d.ts.map