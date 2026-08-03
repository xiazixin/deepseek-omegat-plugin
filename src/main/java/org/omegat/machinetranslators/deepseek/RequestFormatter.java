package org.omegat.machinetranslators.deepseek;

import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Formats a DeepSeek API request body for human-readable display in the
 * "Current prompts" viewer: request parameters as "key: value" lines,
 * followed by each message under its role. Escaped sequences in the message
 * content (\n, \t, ...) are rendered as real line breaks and spacing.
 */
final class RequestFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RequestFormatter() {
    }

    static String format(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            StringBuilder sb = new StringBuilder();

            // Request parameters (model, temperature, stream, ...)
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (!"messages".equals(field.getKey()) && field.getValue().isValueNode()) {
                    sb.append(field.getKey()).append(": ").append(field.getValue().asText()).append('\n');
                }
            }

            JsonNode messages = root.get("messages");
            if (messages != null && messages.isArray()) {
                for (JsonNode message : messages) {
                    String role = message.path("role").asText("message");
                    String content = message.path("content").asText("");
                    sb.append('\n').append("=== ").append(role).append(" ===\n");
                    sb.append(content).append('\n');
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return json; // Not parseable — show the body as-is
        }
    }
}
