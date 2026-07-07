export type SkillListJsonItem = {
    id: string;
    digest: string;
    files?: number;
    summary?: string;
    version?: string;
};
export type SkillListJson = {
    skillflag_version: "0.1";
    skills: SkillListJsonItem[];
};
type SkillInfo = {
    id: string;
    dir: string;
    summary?: string;
    version?: string;
};
export declare function listSkills(rootDirs: string[]): Promise<SkillInfo[]>;
export declare function listSkillsJson(rootDirs: string[]): Promise<SkillListJson>;
export {};
//# sourceMappingURL=list.d.ts.map