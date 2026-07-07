function stripYamlQuotes(value) {
    if ((value.startsWith('"') && value.endsWith('"')) ||
        (value.startsWith("'") && value.endsWith("'"))) {
        return value.slice(1, -1).trim();
    }
    return value;
}
export function parseFrontmatter(content) {
    const frontmatterMatch = content.match(/^---\s*\r?\n([\s\S]*?)\r?\n---(?:\r?\n|$)/);
    if (!frontmatterMatch) {
        return {};
    }
    const block = frontmatterMatch[1];
    const lines = block.split(/\r?\n/).filter((line) => line.trim().length > 0);
    const fields = {};
    for (const line of lines) {
        const idx = line.indexOf(":");
        if (idx === -1)
            continue;
        const key = line.slice(0, idx).trim();
        const value = stripYamlQuotes(line.slice(idx + 1).trim());
        if (key && value) {
            fields[key] = value;
        }
    }
    return fields;
}
//# sourceMappingURL=frontmatter.js.map