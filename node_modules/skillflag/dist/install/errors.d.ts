export declare class InstallError extends Error {
    readonly exitCode: number;
    constructor(message: string, exitCode?: number);
}
export declare function toErrorMessage(err: unknown): string;
//# sourceMappingURL=errors.d.ts.map