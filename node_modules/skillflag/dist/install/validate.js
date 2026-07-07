import fs from "node:fs/promises";
import path from "node:path";
import { InstallError } from "./errors.js";
import { parseFrontmatter } from "../shared/frontmatter.js";
export async function assertSkillDir(rootDir) {
    const skillMd = path.join(rootDir, "SKILL.md");
    try {
        await fs.access(skillMd);
    }
    catch {
        throw new InstallError("SKILL.md not found in skill root.");
    }
}
export async function readSkillMetadata(rootDir) {
    const skillMdPath = path.join(rootDir, "SKILL.md");
    const content = await fs.readFile(skillMdPath, "utf8");
    const fields = parseFrontmatter(content);
    const name = fields.name;
    const description = fields.description;
    if (!name) {
        throw new InstallError("SKILL.md metadata is missing name.");
    }
    if (!description) {
        throw new InstallError("SKILL.md metadata is missing description.");
    }
    return { name, description };
}
//# sourceMappingURL=validate.js.map