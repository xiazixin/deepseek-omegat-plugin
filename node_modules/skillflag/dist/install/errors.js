export class InstallError extends Error {
    exitCode;
    constructor(message, exitCode = 1) {
        super(message);
        this.exitCode = exitCode;
    }
}
export function toErrorMessage(err) {
    if (err instanceof Error)
        return err.message;
    return String(err);
}
//# sourceMappingURL=errors.js.map