import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { assertSkillDir, readSkillMetadata } from "./validate.js";
import { extractSkillTarToTemp } from "./extract.js";
import { resolveSkillsRoot } from "./resolve.js";
import { copySkillDir } from "./copy.js";
export async function installSkill(input, options) {
    const { agent, scope, cwd, force } = options;
    let rootDir = "";
    let cleanup = async () => { };
    if (input.kind === "dir") {
        rootDir = path.resolve(input.dir);
    }
    else {
        const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), "skill-install-"));
        cleanup = async () => {
            await fs.rm(tempDir, { recursive: true, force: true });
        };
        rootDir = await extractSkillTarToTemp(input.stream, tempDir);
    }
    try {
        await assertSkillDir(rootDir);
        const meta = await readSkillMetadata(rootDir);
        const skillId = meta.name;
        const skillsRoot = resolveSkillsRoot(agent, scope, cwd);
        const destDir = path.join(skillsRoot, skillId);
        await copySkillDir(rootDir, destDir, force);
        return { skillId, installedTo: destDir };
    }
    finally {
        await cleanup();
    }
}
//# sourceMappingURL=install.js.map