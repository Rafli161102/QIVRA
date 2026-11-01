package dev.qivra.analyzer;

import java.util.List;
import java.util.Map;

final class JsonWriter {
    private JsonWriter() {
    }

    static String write(Object value, boolean pretty) {
        StringBuilder output = new StringBuilder();
        append(value, output, pretty, 0);
        return output.append('\n').toString();
    }

    private static void append(Object value, StringBuilder output, boolean pretty, int depth) {
        if (value == null) {
            output.append("null");
            return;
        }
        if (value instanceof String text) {
            quote(text, output);
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            output.append(value);
            return;
        }
        if (value instanceof Map<?, ?> map) {
            output.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    output.append(',');
                }
                newline(output, pretty, depth + 1);
                quote(String.valueOf(entry.getKey()), output);
                output.append(pretty ? ": " : ":");
                append(entry.getValue(), output, pretty, depth + 1);
                first = false;
            }
            if (!first) {
                newline(output, pretty, depth);
            }
            output.append('}');
            return;
        }
        if (value instanceof List<?> items) {
            output.append('[');
            for (int index = 0; index < items.size(); index++) {
                if (index > 0) {
                    output.append(',');
                }
                newline(output, pretty, depth + 1);
                append(items.get(index), output, pretty, depth + 1);
            }
            if (!items.isEmpty()) {
                newline(output, pretty, depth);
            }
            output.append(']');
            return;
        }
        quote(String.valueOf(value), output);
    }

    private static void newline(StringBuilder output, boolean pretty, int depth) {
        if (pretty) {
            output.append('\n').append("  ".repeat(depth));
        }
    }

    private static void quote(String value, StringBuilder output) {
        output.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> {
                    if (character < 0x20) {
                        output.append(String.format("\\u%04x", (int) character));
                    } else {
                        output.append(character);
                    }
                }
            }
        }
        output.append('"');
    }
}
