import { z } from "zod/v4";
const skippedItem = Symbol("skippedItem");
export function defaultOnError(schema, fallback) {
    return schema.catch(fallback);
}
export function requiredDefaultOnError(schema, fallback) {
    const schemaWithCatch = schema.catch(fallback);
    return z.unknown().transform((value, context) => {
        if (value !== undefined)
            return schemaWithCatch.parse(value);
        context.addIssue({
            code: "custom",
            message: "Required value is missing",
        });
        return z.NEVER;
    });
}
export function vecSkipError(itemSchema) {
    return z
        .array(itemSchema.catch(skippedItem))
        .transform((items) => items.filter((item) => item !== skippedItem));
}
//# sourceMappingURL=schema-deserialize.js.map