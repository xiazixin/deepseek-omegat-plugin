export type SkillMetadata = {
    name: string;
    description: string;
};
export declare function assertSkillDir(rootDir: string): Promise<void>;
export declare function readSkillMetadata(rootDir: string): Promise<SkillMetadata>;
//# sourceMappingURL=validate.d.ts.map