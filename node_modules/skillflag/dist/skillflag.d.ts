import type { Readable, Writable } from "node:stream";
import type { Option } from "@clack/prompts";
import { type SkillsRootInput } from "./core/paths.js";
export type SkillflagOptions = {
    skillsRoot: SkillsRootInput | readonly SkillsRootInput[];
    stdin?: NodeJS.ReadableStream;
    stdout?: NodeJS.WritableStream;
    stderr?: NodeJS.WritableStream;
    cwd?: string;
    includeBundledSkill?: boolean;
    promptApi?: SkillflagPromptApi;
};
export type SkillflagDispatchOptions = SkillflagOptions & {
    exit?: ((code: number) => void) | false;
};
export type SkillflagPromptApi = {
    multiselect: <Value>(opts: {
        message: string;
        options: Option<Value>[];
        required?: boolean;
        input?: Readable;
        output?: Writable;
    }) => Promise<Value[] | symbol>;
    isCancel: (value: unknown) => value is symbol;
};
export declare const SKILLFLAG_HELP_TEXT: string;
export declare function handleSkillflag(argv: string[], opts: SkillflagOptions): Promise<number>;
export declare function maybeHandleSkillflag(argv: string[], opts: SkillflagDispatchOptions): Promise<boolean>;
//# sourceMappingURL=skillflag.d.ts.map