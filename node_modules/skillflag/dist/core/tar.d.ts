import { Readable } from "node:stream";
type TarEntry = {
    name: string;
    type: "file" | "directory";
    absPath?: string;
    size?: number;
    mode: number;
};
export type CollectedSkillEntries = {
    entries: TarEntry[];
    fileCount: number;
};
export declare function collectSkillEntries(skillDir: string, id: string): Promise<CollectedSkillEntries>;
export declare function createTarStream(entries: TarEntry[]): Readable;
export {};
//# sourceMappingURL=tar.d.ts.map