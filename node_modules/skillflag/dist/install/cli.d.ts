import { Readable, Writable } from "node:stream";
import { confirm, text } from "@clack/prompts";
import type { Option } from "@clack/prompts";
import { type InstallInput } from "./install.js";
export type InstallCliOptions = {
    stdin?: Readable;
    stdout?: Writable;
    stderr?: Writable;
    cwd?: string;
    openPromptTty?: () => PromptTtyStreams | null;
    providedInput?: InstallInput;
    providedInputs?: InstallInput[];
    providedSkillId?: string;
    providedSkillIds?: string[];
    promptApi?: InstallPromptApi;
};
export type InstallPromptApi = {
    confirm: (opts: Parameters<typeof confirm>[0]) => Promise<boolean | symbol>;
    intro: (message?: string, opts?: {
        input?: Readable;
        output?: Writable;
    }) => void;
    isCancel: (value: unknown) => value is symbol;
    multiselect: <Value>(opts: {
        message: string;
        options: Option<Value>[];
        initialValues?: Value[];
        required?: boolean;
        input?: Readable;
        output?: Writable;
    }) => Promise<Value[] | symbol>;
    note: (message?: string, title?: string, opts?: {
        input?: Readable;
        output?: Writable;
    }) => void;
    outro: (message?: string, opts?: {
        input?: Readable;
        output?: Writable;
    }) => void;
    spinner: (opts?: {
        input?: Readable;
        output?: Writable;
    }) => {
        start: (message?: string) => void;
        stop: (message?: string) => void;
        error: (message?: string) => void;
    };
    text: (opts: Parameters<typeof text>[0]) => Promise<string | symbol>;
};
type PromptTtyStreams = {
    input: Readable;
    output: Writable;
    close: () => void;
};
export declare function runInstallCli(argv: string[], opts?: InstallCliOptions): Promise<number>;
export {};
//# sourceMappingURL=cli.d.ts.map