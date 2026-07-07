import fs from "node:fs/promises";
import path from "node:path";
export async function showSkill(skillDir, _id, stdout) {
    const skillMdPath = path.join(skillDir, "SKILL.md");
    const content = await fs.readFile(skillMdPath, "utf8");
    stdout.write(content);
}
//# sourceMappingURL=show.js.map