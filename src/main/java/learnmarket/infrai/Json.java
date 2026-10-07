package learnmarket.infrai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() {}

    public static Object parse(String text) {
        Parser parser = new Parser(text);
        Object value = parser.value();
        parser.space();
        if (!parser.done()) throw new IllegalArgumentException("Unexpected JSON after value");
        return value;
    }

    public static String stringify(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return quote(s);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append(quote(entry.getKey().toString())).append(':').append(stringify(entry.getValue()));
            }
            return out.append('}').toString();
        }
        if (value instanceof Iterable<?> items) {
            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            for (Object item : items) {
                if (!first) out.append(',');
                first = false;
                out.append(stringify(item));
            }
            return out.append(']').toString();
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass().getName());
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 32) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('\"').toString();
    }

    private static final class Parser {
        private final String text;
        private int at;

        private Parser(String text) { this.text = text; }
        private boolean done() { return at == text.length(); }
        private void space() { while (!done() && Character.isWhitespace(text.charAt(at))) at++; }

        private Object value() {
            space();
            if (done()) throw problem("Expected value");
            return switch (text.charAt(at)) {
                case '{' -> object();
                case '[' -> array();
                case '\"' -> string();
                case 't' -> literal("true", true);
                case 'f' -> literal("false", false);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            Map<String, Object> result = new LinkedHashMap<>();
            at++;
            space();
            if (take('}')) return result;
            do {
                space();
                if (done() || text.charAt(at) != '\"') throw problem("Expected object key");
                String key = string();
                space();
                expect(':');
                result.put(key, value());
                space();
            } while (take(','));
            expect('}');
            return result;
        }

        private List<Object> array() {
            List<Object> result = new ArrayList<>();
            at++;
            space();
            if (take(']')) return result;
            do {
                result.add(value());
                space();
            } while (take(','));
            expect(']');
            return result;
        }

        private String string() {
            expect('\"');
            StringBuilder out = new StringBuilder();
            while (!done()) {
                char c = text.charAt(at++);
                if (c == '\"') return out.toString();
                if (c != '\\') { out.append(c); continue; }
                if (done()) throw problem("Unfinished escape");
                char escape = text.charAt(at++);
                switch (escape) {
                    case '\"', '\\', '/' -> out.append(escape);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (at + 4 > text.length()) throw problem("Unfinished unicode escape");
                        out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
                        at += 4;
                    }
                    default -> throw problem("Unknown escape");
                }
            }
            throw problem("Unclosed string");
        }

        private Object number() {
            int start = at;
            if (take('-')) { }
            while (!done() && Character.isDigit(text.charAt(at))) at++;
            if (take('.')) while (!done() && Character.isDigit(text.charAt(at))) at++;
            if (!done() && (text.charAt(at) == 'e' || text.charAt(at) == 'E')) {
                at++;
                if (!done() && (text.charAt(at) == '+' || text.charAt(at) == '-')) at++;
                while (!done() && Character.isDigit(text.charAt(at))) at++;
            }
            if (start == at) throw problem("Expected number");
            String token = text.substring(start, at);
            return token.contains(".") || token.contains("e") || token.contains("E")
                    ? Double.parseDouble(token) : Long.parseLong(token);
        }

        private Object literal(String token, Object value) {
            if (!text.startsWith(token, at)) throw problem("Expected " + token);
            at += token.length();
            return value;
        }

        private boolean take(char wanted) {
            if (!done() && text.charAt(at) == wanted) { at++; return true; }
            return false;
        }

        private void expect(char wanted) {
            if (!take(wanted)) throw problem("Expected " + wanted);
        }

        private IllegalArgumentException problem(String message) {
            return new IllegalArgumentException(message + " at character " + at);
        }
    }
}
