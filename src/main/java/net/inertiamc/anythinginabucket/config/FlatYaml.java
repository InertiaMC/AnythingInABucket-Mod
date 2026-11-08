package net.inertiamc.anythinginabucket.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal YAML reader covering what config.yml uses, so no YAML library has to be bundled: nested sections,
 * {@code - item} lists, {@code []}, flow maps such as {@code { name: x, volume: 1.0 }} and flow lists, quoted or plain
 * scalars, and comments. Errors carry the line number.
 */
public final class FlatYaml {

    private record Line(int number, int indent, String text) {}

    private final List<Line> lines = new ArrayList<>();
    private int at;

    private FlatYaml(String source) {
        String[] raw = source.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (int i = 0; i < raw.length; i++) {
            String text = stripComment(raw[i]);
            if (text.isBlank()) continue;
            if (text.indexOf('\t') >= 0 && text.substring(0, indentOf(text)).indexOf('\t') >= 0) {
                throw new IllegalArgumentException("line " + (i + 1) + ": use spaces, not tabs, to indent");
            }
            lines.add(new Line(i + 1, indentOf(text), text.strip()));
        }
    }

    public static Map<String, Object> parse(String source) {
        FlatYaml yaml = new FlatYaml(source);
        if (yaml.lines.isEmpty()) return new LinkedHashMap<>();
        Object root = yaml.block(yaml.lines.get(0).indent());
        if (yaml.at < yaml.lines.size()) throw yaml.error(yaml.lines.get(yaml.at), "this line is indented oddly");
        if (!(root instanceof Map<?, ?>)) throw new IllegalArgumentException("the file is not a list of settings");
        @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) root;
        return map;
    }

    /** A section or list whose lines start at {@code indent}. */
    private Object block(int indent) {
        Line first = lines.get(at);
        return first.text().startsWith("- ") || first.text().equals("-") ? list(indent) : map(indent);
    }

    private Map<String, Object> map(int indent) {
        Map<String, Object> map = new LinkedHashMap<>();
        while (at < lines.size()) {
            Line line = lines.get(at);
            if (line.indent() < indent) break;
            if (line.indent() > indent) throw error(line, "this line is indented oddly");
            if (line.text().startsWith("- ")) throw error(line, "a list item where a setting was expected");
            int colon = keyColon(line.text());
            if (colon < 0) throw error(line, "expected 'name: value'");
            String key = unquote(line.text().substring(0, colon).strip());
            String rest = line.text().substring(colon + 1).strip();
            at++;
            if (!rest.isEmpty()) {
                map.put(key, scalarOrFlow(rest, line));
            } else if (at < lines.size() && lines.get(at).indent() > indent) {
                map.put(key, block(lines.get(at).indent()));
            } else if (at < lines.size() && lines.get(at).indent() == indent && lines.get(at).text().startsWith("- ")) {
                map.put(key, list(indent));
            } else {
                map.put(key, null);
            }
        }
        return map;
    }

    private List<Object> list(int indent) {
        List<Object> list = new ArrayList<>();
        while (at < lines.size()) {
            Line line = lines.get(at);
            if (line.indent() != indent || !(line.text().startsWith("- ") || line.text().equals("-"))) break;
            String item = line.text().equals("-") ? "" : line.text().substring(2).strip();
            at++;
            list.add(item.isEmpty() ? null : scalarOrFlow(item, line));
        }
        return list;
    }

    private Object scalarOrFlow(String text, Line line) {
        if (text.startsWith("{") || text.startsWith("[")) {
            int[] pos = {0};
            Object value = flow(text, pos, line);
            if (!text.substring(pos[0]).isBlank()) throw error(line, "text after the closing bracket");
            return value;
        }
        return scalar(text);
    }

    private Object flow(String text, int[] pos, Line line) {
        skipSpaces(text, pos);
        char open = text.charAt(pos[0]);
        if (open == '{') {
            pos[0]++;
            Map<String, Object> map = new LinkedHashMap<>();
            while (true) {
                skipSpaces(text, pos);
                if (pos[0] >= text.length()) throw error(line, "a '{' is never closed");
                if (text.charAt(pos[0]) == '}') { pos[0]++; return map; }
                String key = token(text, pos, ":", line);
                if (pos[0] >= text.length() || text.charAt(pos[0]) != ':') throw error(line, "expected ':' after '" + key + "'");
                pos[0]++;
                skipSpaces(text, pos);
                Object value = text.charAt(pos[0]) == '{' || text.charAt(pos[0]) == '[' ? flow(text, pos, line) : scalar(token(text, pos, ",}", line));
                map.put(unquote(key), value);
                skipSpaces(text, pos);
                if (pos[0] < text.length() && text.charAt(pos[0]) == ',') pos[0]++;
            }
        }
        if (open == '[') {
            pos[0]++;
            List<Object> list = new ArrayList<>();
            while (true) {
                skipSpaces(text, pos);
                if (pos[0] >= text.length()) throw error(line, "a '[' is never closed");
                if (text.charAt(pos[0]) == ']') { pos[0]++; return list; }
                Object value = text.charAt(pos[0]) == '{' || text.charAt(pos[0]) == '[' ? flow(text, pos, line) : scalar(token(text, pos, ",]", line));
                list.add(value);
                skipSpaces(text, pos);
                if (pos[0] < text.length() && text.charAt(pos[0]) == ',') pos[0]++;
            }
        }
        throw error(line, "expected '{' or '['");
    }

    /** Text up to one of {@code stops}, outside quotes. */
    private String token(String text, int[] pos, String stops, Line line) {
        skipSpaces(text, pos);
        int start = pos[0];
        char quote = 0;
        while (pos[0] < text.length()) {
            char c = text.charAt(pos[0]);
            if (quote != 0) {
                if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (stops.indexOf(c) >= 0) {
                break;
            }
            pos[0]++;
        }
        if (quote != 0) throw error(line, "a quote is never closed");
        return text.substring(start, pos[0]).strip();
    }

    private static Object scalar(String text) {
        String t = text.strip();
        if (t.isEmpty() || t.equals("~") || t.equals("null")) return null;
        if (t.length() >= 2 && (t.startsWith("\"") && t.endsWith("\"") || t.startsWith("'") && t.endsWith("'"))) return unquote(t);
        if (t.equals("true")) return Boolean.TRUE;
        if (t.equals("false")) return Boolean.FALSE;
        try {
            if (t.matches("-?\\d+")) return Long.parseLong(t) <= Integer.MAX_VALUE && Long.parseLong(t) >= Integer.MIN_VALUE ? (Object) Integer.parseInt(t) : (Object) Long.parseLong(t);
            if (t.matches("-?\\d*\\.\\d+")) return Double.parseDouble(t);
        } catch (NumberFormatException ignored) {
            // falls through to a string
        }
        return t;
    }

    private static String unquote(String text) {
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            return text.substring(1, text.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        if (text.length() >= 2 && text.startsWith("'") && text.endsWith("'")) {
            return text.substring(1, text.length() - 1).replace("''", "'");
        }
        return text;
    }

    /** The colon ending a key, outside quotes and followed by a space or the end; -1 when there is none. */
    private static int keyColon(String text) {
        char quote = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == ':' && (i + 1 == text.length() || text.charAt(i + 1) == ' ')) {
                return i;
            }
        }
        return -1;
    }

    private static String stripComment(String raw) {
        char quote = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '#' && (i == 0 || Character.isWhitespace(raw.charAt(i - 1)))) {
                return raw.substring(0, i);
            }
        }
        return raw;
    }

    private static int indentOf(String text) {
        int i = 0;
        while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) i++;
        return i;
    }

    private static void skipSpaces(String text, int[] pos) {
        while (pos[0] < text.length() && text.charAt(pos[0]) == ' ') pos[0]++;
    }

    private IllegalArgumentException error(Line line, String what) {
        return new IllegalArgumentException("line " + line.number() + ": " + what);
    }
}
