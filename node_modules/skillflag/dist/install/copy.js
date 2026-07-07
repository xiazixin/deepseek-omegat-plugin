import fs from "node:fs/promises";
import path from "node:path";
import { InstallError } from "./errors.js";
export async function copySkillDir(sourceDir, destDir, force) {
    try {
        await fs.access(destDir);
        if (!force) {
            throw new InstallError(`Destination already exists: ${destDir}`);
        }
        await fs.rm(destDir, { recursive: true, force: true });
    }
    catch (err) {
        if (err instanceof InstallError)
            throw err;
    }
    await fs.mkdir(path.dirname(destDir), { recursive: true });
    await fs.cp(sourceDir, destDir, { recursive: true });
}
//# sourceMappingURL=copy.js.map