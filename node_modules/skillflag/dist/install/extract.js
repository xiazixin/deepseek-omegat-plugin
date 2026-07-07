import fs from "node:fs/promises";
import path from "node:path";
import * as tar from "tar-stream";
import { InstallError } from "./errors.js";
function isInvalidRelPath(relPosix) {
    if (path.posix.isAbsolute(relPosix))
        return true;
    const parts = relPosix.split("/");
    return parts.includes("..") || parts.some((part) => part.length === 0);
}
export async function extractSkillTarToTemp(stream, tempDir) {
    const extract = tar.extract();
    let rootName = null;
    const done = new Promise((resolve, reject) => {
        extract.on("entry", async (header, entryStream, next) => {
            try {
                const rawName = header.name;
                if (!rawName || rawName.includes("\\")) {
                    throw new InstallError(`Invalid path in tar: ${rawName}`);
                }
                const name = rawName.endsWith("/") ? rawName.slice(0, -1) : rawName;
                if (!name || isInvalidRelPath(name)) {
                    throw new InstallError(`Invalid path in tar: ${rawName}`);
                }
                const [top, ...rest] = name.split("/");
                if (!top) {
                    throw new InstallError(`Invalid tar entry name: ${rawName}`);
                }
                if (!rootName)
                    rootName = top;
                if (rootName !== top) {
                    throw new InstallError("Tar must contain a single top-level directory.");
                }
                const relPath = rest.join("/");
                const absPath = relPath
                    ? path.join(tempDir, top, relPath)
                    : path.join(tempDir, top);
                if (header.type === "directory") {
                    await fs.mkdir(absPath, { recursive: true });
                    entryStream.resume();
                    entryStream.on("end", next);
                    return;
                }
                if (header.type === "file") {
                    if (!relPath) {
                        throw new InstallError("Tar must contain a single top-level directory.");
                    }
                    await fs.mkdir(path.dirname(absPath), { recursive: true });
                    const chunks = [];
                    entryStream.on("data", (chunk) => chunks.push(Buffer.from(chunk)));
                    entryStream.on("end", async () => {
                        await fs.writeFile(absPath, Buffer.concat(chunks));
                        next();
                    });
                    return;
                }
                throw new InstallError(`Unsupported tar entry type: ${header.type}`);
            }
            catch (err) {
                entryStream.resume();
                reject(err);
            }
        });
        extract.on("finish", () => {
            if (!rootName) {
                reject(new InstallError("Tar stream was empty."));
                return;
            }
            resolve(path.join(tempDir, rootName));
        });
        extract.on("error", (err) => reject(err));
    });
    stream.pipe(extract);
    return done;
}
//# sourceMappingURL=extract.js.map