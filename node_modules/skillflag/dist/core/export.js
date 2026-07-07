import { collectSkillEntries, createTarStream } from "./tar.js";
async function pipeToWritable(stream, dest) {
    return new Promise((resolve, reject) => {
        const onError = (err) => {
            cleanup();
            reject(err);
        };
        const onEnd = () => {
            cleanup();
            resolve();
        };
        const cleanup = () => {
            stream.removeListener("error", onError);
            stream.removeListener("end", onEnd);
            dest.removeListener("error", onError);
        };
        stream.on("error", onError);
        stream.on("end", onEnd);
        dest.on("error", onError);
        stream.pipe(dest, { end: false });
    });
}
export async function exportSkill(skillDir, id, stdout) {
    const { entries } = await collectSkillEntries(skillDir, id);
    const tarStream = createTarStream(entries);
    await pipeToWritable(tarStream, stdout);
}
//# sourceMappingURL=export.js.map