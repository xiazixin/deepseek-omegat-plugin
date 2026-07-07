import { z } from "zod/v4";
type Fallback<Output> = () => Output;
export declare function defaultOnError<Schema extends z.ZodType>(schema: Schema, fallback: Fallback<z.output<Schema>>): z.ZodCatch<Schema>;
export declare function requiredDefaultOnError<Schema extends z.ZodType>(schema: Schema, fallback: Fallback<z.output<Schema>>): z.ZodType<z.output<Schema>, z.input<Schema>>;
export declare function vecSkipError<ItemSchema extends z.ZodType>(itemSchema: ItemSchema): z.ZodPipe<z.ZodArray<z.ZodCatch<ItemSchema>>, z.ZodTransform<z.core.output<ItemSchema>[], z.core.output<ItemSchema>[]>>;
export {};
