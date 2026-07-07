export type SkillsRootInput = URL | string;
export type SkillDir = {
    id: string;
    dir: string;
};
export declare function defaultSkillsRoot(): URL;
export declare function resolveSkillsRoot(root: SkillsRootInput): string;
export declare function resolveSkillsRoots(roots: SkillsRootInput | readonly SkillsRootInput[]): string[];
export declare function findSkillsRoots(start: SkillsRootInput): URL[];
export declare function findSkillsRoot(start: SkillsRootInput): URL;
export declare function assertValidSkillId(id: string): void;
export declare function listSkillDirs(rootDir: string): Promise<SkillDir[]>;
export declare function resolveSkillDir(rootDir: string, id: string): Promise<string>;
export declare function resolveSkillDirFromRoots(rootDirs: string[], id: string): Promise<string>;
//# sourceMappingURL=paths.d.ts.map