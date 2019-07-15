/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

/*
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.regex.Matcher;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * plain yaml parser class which is a parent of YamlParser class.
 */
public class PlainYamlParser implements Parser {

    private static final String ANCHOR = "anchor '";
    private static final String ENDFLAG_EOF       = "<EOF>";
    private static final String ENDFLAG_DOC_BEGIN = "---";
    private static final String ENDFLAG_DOC_END   = "...";
    private static final String REGEXP1 = "^( *)(.*)";
    private static final String REGEXP2 = "^((?::?[-.\\w]+|'.*?'|\".*?\"|=|<<) *):(( +)(.*))?$";

    public static class Alias {
        private String label;
        private int lineNum;

        Alias(String label, int lineNum) {
            this.label = label;
            this.lineNum = lineNum;
        }

        String getLabel() { return label; }

        int getLineNumber() { return lineNum; }
    }


    private String[] lines;
    private String line = null;
    private int linenum = 0;
    private Map<String,Object> anchors = new HashMap<>();
    private Map<String,Integer> aliases = new HashMap<>();
    private String endFlag = null;
    private String sbuf = null;
    private int index = 0;

    PlainYamlParser(String yamlStr) {
        List list = Util.toListOfLines(yamlStr);
        int len = list.size();
        lines = new String[len + 1];
        for (int i = 0; i < len; i++) {
            lines[i + 1] = (String)list.get(i);
        }
    }

    public Object parse() throws SyntaxException {
        Object data = parseChild(0);
        if (data == null && endFlag.equals(ENDFLAG_DOC_BEGIN)) {
            data = parseChild(0);
        }
        if (aliases.size() > 0) {
            resolveAliases(data);
        }
        return data;
    }

    public boolean hasNext() {
        return !endFlag.equals(ENDFLAG_EOF);
    }

    private List createSequence() {
        return new ArrayList();
    }

    private void addSequenceValue(List seq, Object value) {
        seq.add(value);
    }

    private void setSequenceValueAt(List seq, int index, Object value) {
        seq.set(index, value);
    }

    private void setMappingValueWith(Map map, Object key, Object value) {
        map.put(key, value);
    }

    void setMappingDefault(Map map, Object value) {
        if (map instanceof Defaultable) {
            ((Defaultable)map).setDefault((Rule)value);
        }
    }

    private void mergeMapping(Map map, Map map2) {
        for (Object key : map2.keySet()) {
            if (!map.containsKey(key)) {
                Object value = map2.get(key);
                map.put(key, value);
            }
        }
    }

    private void mergeList(Map map, List maplist) throws SyntaxException {
        for (Object elem : maplist) {
            mergeCollection(map, elem);
        }
    }

    private void mergeCollection(Map map, Object collection) throws SyntaxException {
        if (collection instanceof Map) {
            mergeMapping(map, (Map)collection);
        } else if (collection instanceof List) {
            mergeList(map, (List)collection);
        } else {
            throw syntaxError("'<<' requires collection (mapping, or sequence of mapping).");
        }
    }

    private Object createScalar(Object value) {
        return value;
    }

    private String currentLine() {
        return line;
    }

    int currentLineNumber() {
        return linenum;
    }

    protected String getLine() {
        String currentLine;
        do {
            currentLine = getCurrentLine();
        } while (currentLine != null && Util.matches(currentLine, "^\\s*($|#)"));
        return currentLine;
    }

    private String getCurrentLine() {
        if (++linenum < lines.length) {
            line = lines[linenum];
            if (Util.matches(line, "^\\.\\.\\.$")) {
                line = null;
                endFlag = ENDFLAG_DOC_END;
            } else if (Util.matches(line, "^---( [!%].*)?$")) {
                line = null;
                endFlag = ENDFLAG_DOC_BEGIN;
            }
        } else {
            line = null;
            endFlag = ENDFLAG_EOF;
        }
        return line;
    }

    private void resetBuffer(String str) {
        sbuf = str.charAt(str.length() - 1) == '\n' ? str : str + "\n";
        index = -1;
    }

    private int getCurrentCharacter() {
        if (index + 1 < sbuf.length()) {
            index++;
        } else {
            String currentLine = getLine();
            if (currentLine == null) {
                return -1;
            }
            resetBuffer(currentLine);
            index++;
        }
        return sbuf.charAt(index);
    }

    private int getChar() {
        int ch;
        do {
            ch = getCurrentCharacter();
        } while (ch >= 0 && isWhite(ch));
        return ch;
    }

    private int getCharOrNewline() {
        int ch;
        do {
            ch = getCurrentCharacter();
        } while (ch >= 0 && isWhite(ch) && ch != '\n');
        return ch;
    }

    private int currentChar() {
        return sbuf.charAt(index);
    }

    private SyntaxException syntaxError(String message, int linenum) {
        return new YamlSyntaxException(message, linenum);
    }

    private SyntaxException syntaxError(String message) {
        return new SyntaxException(message, linenum);
    }

    private Object parseChild(int column) throws SyntaxException {
        String currentLine = getLine();
        if (currentLine == null) {
            return createScalar(null);
        }
        Matcher m = Util.matcher(currentLine, REGEXP1);
        if (! m.find()) {
            assert false;
            return null;
        }
        int indent = m.group(1).length();
        if (indent < column) {
            return createScalar(null);
        }
        String value = m.group(2);
        return parseValue(column, value, indent);
    }

    private Object parseValue(int column, String value, int valueStartColumn) throws SyntaxException {
        Object data;
        if        (Util.matches(value, "^-( |$)")) {
            data = parseSequence(valueStartColumn, value);
        } else if (Util.matches(value, REGEXP2)) {
            data = parseMapping(valueStartColumn, value);
        } else if (Util.matches(value, "^[\\[\\{]")) {
            data = parseFlowStyle(value);
        } else if (Util.matches(value, "^\\&[-\\w]+( |$)")) {
            data = parseAnchor(column, value);
        } else if (Util.matches(value, "^\\*[-\\w]+( |$)")) {
            data = parseAlias(value);
        } else if (Util.matches(value, "^[|>]")) {
            data = parseBlockText(column, value);
        } else if (Util.matches(value, "^!")) {
            data = parseTag(column, value);
        } else if (Util.matches(value, "^\\#")) {
            data = parseChild(column);
        } else {
            data = parseScalar(value);
        }
        return data;
    }

    private static boolean isWhite(int ch) {
        return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r';
    }


    private Object parseFlowStyle(String value) throws SyntaxException {
        resetBuffer(value);
        getChar();
        Object data = parseFlow(0);
        int ch = currentChar();
        assert ch == ']' || ch == '}';
        ch = getCharOrNewline();
        if (ch != '\n' && ch != '#' && ch >= 0) {
            throw syntaxError("flow style sequence is closed buf got '" + ((char)ch) + "'.");
        }
        if (ch >= 0) {
            getLine();
        }
        return data;
    }

    private Object parseFlow(int depth) throws SyntaxException {
        int ch = currentChar();
        if (ch < 0) {
            throw syntaxError("found EOF when parsing flow style.");
        }
        Object data;
        if (ch == '[') {
            data = parseFlowSequence(depth);
        } else if (ch == '{') {
            data = parseFlowMapping(depth);
        } else {
            data = parseFlowScalar();
        }
        return data;
    }

    private List parseFlowSequence(int depth) throws SyntaxException {
        assert currentChar() == '[';
        List seq = createSequence();
        int ch = getChar();
        if (ch != '}') {
            addSequenceValue(seq, parseFlowSequenceItem(depth + 1));
            while ((ch = currentChar()) == ',') {
                ch = getChar();
                if (ch == '}') {
                    throw syntaxError("sequence item required (or last comma is extra).");
                }
                addSequenceValue(seq, parseFlowSequenceItem(depth + 1));
            }
        }
        if (currentChar() != ']') {
            throw syntaxError("flow style sequence requires ']'.");
        }
        if (depth > 0) {
            getChar();
        }
        return seq;
    }

    private Object parseFlowSequenceItem(int depth) throws SyntaxException {
        return parseFlow(depth);
    }

    private Map parseFlowMapping(int depth) throws SyntaxException {
        assert currentChar() == '{';
        Map map = new DefaultableHashMap();
        int ch = getChar();
        if (ch != '}') {
            Object[] pair = parseFlowMappingItem(depth + 1);
            Object key   = pair[0];
            Object value = pair[1];
            setMappingValueWith(map, key, value);
            while ((currentChar()) == ',') {
                ch = getChar();
                if (ch == '}') {
                    throw syntaxError("mapping item required (or last comman is extra.");
                }
                pair = parseFlowMappingItem(depth + 1);
                key   = pair[0];
                value = pair[1];
                setMappingValueWith(map, key, value);
            }
        }
        if (currentChar() != '}') {
            throw syntaxError("flow style mapping requires '}'.");
        }
        if (depth > 0) {
            getChar();
        }
        return map;
    }

    private Object[] parseFlowMappingItem(int depth) throws SyntaxException {
        Object key = parseFlow(depth);
        int ch = currentChar();
        if (ch != ':') {
            String s = ch >= 0 ? "'" + ((char)ch) + "'" : "EOF";
            throw syntaxError("':' expected but got " + s);
        }
        getChar();
        Object value = parseFlow(depth);
        return new Object[] { key, value };
    }

    private Object parseFlowScalar() {
        int ch = currentChar();
        Object scalar;
        StringBuilder sb = new StringBuilder();
        if (ch == '"' || ch == '\'') {
            int endch = ch;
            while ((ch = getCurrentCharacter()) >= 0 && ch != endch) {
                sb.append((char)ch);
            }
            getChar();
            scalar = sb.toString();
        } else {
            sb.append((char)ch);
            String lookup = ":,]}";
            while ((ch = getCurrentCharacter()) >= 0 && lookup.indexOf(ch) == -1) {
                sb.append((char)ch);
            }
            scalar = toScalar(sb.toString().trim());
        }
        return createScalar(scalar);
    }

    private Object parseTag(int column, String value) throws SyntaxException {
        assert Util.matches(value, "^!\\S+");
        Matcher m = Util.matcher(value, "^!(\\S+)((\\s+)(.*))?$");
        if (! m.find()) {
            assert false;
            return null;
        }
        String tag = m.group(1);
        String space = m.group(3);
        String value2 = m.group(4);
        Object data;
        if (value2 != null && value2.length() > 0) {
            int valueStartColumn = column + 1 + tag.length() + space.length();
            data = parseValue(column, value2, valueStartColumn);
        } else {
            data = parseChild(column);
        }
        return data;
    }

    private Object parseAnchor(int column, String value) throws SyntaxException {
        assert Util.matches(value, "^\\&([-\\w]+)(( *)(.*))?$");
        Matcher m = Util.matcher(value, "^\\&([-\\w]+)(( *)(.*))?$");
        if (! m.find()) {
            assert false;
            return null;
        }
        String label  = m.group(1);
        String space  = m.group(3);
        String value2 = m.group(4);
        Object data;
        if (value2 != null && value2.length() > 0) {
            int valueStartColumn = column + 1 + label.length() + space.length();
            data = parseValue(column, value2, valueStartColumn);
        } else {
            data = parseChild(column);
        }
        registerAnchor(label, data);
        return data;
    }

    private void registerAnchor(String label, Object data) throws SyntaxException {
        if (anchors.containsKey(label)) {
            throw syntaxError(ANCHOR + label + "' is already used.");
        }
        anchors.put(label, data);
    }

    private Object parseAlias(String value) throws SyntaxException {
        assert value.matches("^\\*([-\\w]+)(( *)(.*))?$");
        Matcher m = Util.matcher(value, "^\\*([-\\w]+)(( *)(.*))?$");
        if (! m.find()) {
            assert false;
            return null;
        }
        String label  = m.group(1);
        String value2 = m.group(4);
        if (value2 != null && value2.length() > 0 && value2.charAt(0) != '#') {
            throw syntaxError("alias cannot take any data.");
        }
        Object data = anchors.get(label);
        if (data == null) {
            data = registerAlias(label);
        }
        getLine();
        return data;
    }

    private Alias registerAlias(String label) {
        aliases.merge(label, 1, (a, b) -> a + b);
        return new Alias(label, linenum);
    }


    private void resolveAliases(Object data) throws SyntaxException {
        Map resolved = new IdentityHashMap();
        resolveAliases(data, resolved);
    }


    private void resolveAliases(Object data, Map resolved) throws SyntaxException {
        if (resolved.containsKey(data)) {
            return;
        }
        resolved.put(data, data);
        if (data instanceof List) {
            resolveAliases((List)data, resolved);
        } else if (data instanceof Map) {
            resolveAliases((Map)data, resolved);
        } else {
            assert !(data instanceof Alias);
        }
        if (data instanceof Defaultable) {
            Object defaultValue = ((Defaultable)data).getDefault();
            if (defaultValue != null) {
                resolveAliases(defaultValue, resolved);
            }
        }
    }

    private void resolveAliases(List seq, Map resolved) throws SyntaxException {
        int len = seq.size();
        for (int i = 0; i < len; i++) {
            Object val = seq.get(i);
            if (val instanceof Alias) {
                Alias alias = (Alias)val;
                String label = alias.getLabel();
                if (anchors.containsKey(label)) {
                    setSequenceValueAt(seq, i, anchors.get(label));
                } else {
                    throw syntaxError(ANCHOR + alias.getLabel() + "' not found.");
                }
            } else if (val instanceof List || val instanceof Map) {
                resolveAliases(val, resolved);
            }
        }
    }

    private void resolveAliases(Map map, Map resolved) throws SyntaxException {
        for (Object key : map.keySet()) {
            Object val = map.get(key);
            if (val instanceof Alias) {
                Alias alias = (Alias) val;
                String label = alias.getLabel();
                if (anchors.containsKey(label)) {
                    setMappingValueWith(map, key, anchors.get(label));
                } else {
                    throw syntaxError(ANCHOR + alias.getLabel() + "' not found.", alias.getLineNumber());
                }
            } else if (val instanceof List || val instanceof Map) {
                resolveAliases(val, resolved);
            }
        }
    }

    private Object parseBlockText(int column, String value) throws SyntaxException {
        assert Util.matches(value, "^[>|]");
        Matcher m = Util.matcher(value, "^([>|])([-+]?)(\\d*)\\s*(.*)$");
        if (! m.find()) {
            assert false;
            return null;
        }
        char blockChar = m.group(1).length() > 0 ? m.group(1).charAt(0) : '\0';
        char indicator = m.group(2).length() > 0 ? m.group(2).charAt(0) : '\0';
        int indent     = m.group(3).length() > 0 ? Integer.parseInt(m.group(3)) : -1;
        String text    = m.group(4);
        char sep = blockChar == '|' ? '\n' : ' ';
        String currentLine;
        StringBuilder sb = new StringBuilder();
        int n = 0;
        while ((currentLine = getCurrentLine()) != null) {
            m = Util.matcher(currentLine, "^( *)(.*)$");
            m.find();
            String space = m.group(1);
            String str   = m.group(2);
            if (indent < 0) {
                indent = space.length();
            }
            if (str.length() == 0) {
                n++;
            } else {
                int slen = space.length();
                if (slen < column) {
                    break;
                } else if (slen < indent) {
                    throw syntaxError("invalid indent in block text.");
                } else {
                    n = indentHandler(blockChar, sb, n);
                    str = currentLine.substring(indent);
                }
            }
            sb.append(str);
            if ((blockChar == '>') && (sb.charAt(sb.length() - 1) == '\n')) {
                    sb.setCharAt(sb.length() - 1, ' ');
            }
        }
        if (currentLine != null && Util.matches(currentLine, "^ *#")) {
            getLine();
        }
        processIndicator(blockChar, indicator, sep, sb, n);
        return createScalar(text + sb.toString());
    }

    private void processIndicator(char blockChar, char indicator, char sep, StringBuilder sb, int n) {
        switch (indicator) {
        case '+':
            handlePlus(blockChar, sb, n);
            break;
        case '-':
            handleMinus(sep, sb);
            break;
        default:
            if (blockChar == '>') {
                sb.setCharAt(sb.length() - 1, '\n');
            }
        }
    }

    private int indentHandler(char blockChar, StringBuilder sb, int indent) {
        if (indent > 0) {
            if (blockChar == '>' && sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
            for (int i = 0; i < indent; i++) {
                sb.append('\n');
            }
            return 0;
        }
        return indent;
    }

    private void handleMinus(char sep, StringBuilder sb) {
        if (sb.charAt(sb.length() - 1) == sep) {
            sb.deleteCharAt(sb.length() - 1);
        }
    }

    private void handlePlus(char blockChar, StringBuilder sb, int n) {
        if (n > 0) {
            if (blockChar == '>') {
                sb.setCharAt(sb.length() - 1, '\n');
            }
            for (int i = 0; i < n; i++) {
                sb.append('\n');
            }
        }
    }


    private List parseSequence(int column, String value) throws SyntaxException {
        assert Util.matches(value, "^-(( +)(.*))?$");
        List seq = createSequence();
        while (true) {
            Matcher m = Util.matcher(value, "^-(( +)(.*))?$");
            if (! m.find()) {
                throw syntaxError("sequence item is expected.");
            }
            String space  = m.group(2);
            String value2 = m.group(3);
            int column2   = column + 1;

            Object elem;
            if (value2 == null || value2.length() == 0) {
                elem = parseChild(column2);
            } else {
                int valueStartColumn = column2 + space.length();
                elem = parseValue(column2, value2, valueStartColumn);
            }
            addSequenceValue(seq, elem);

            String currentLine = currentLine();
            if (currentLine == null) {
                break;
            }
            Matcher m2 = Util.matcher(currentLine, REGEXP1);
            m2.find();
            int indent = m2.group(1).length();
            if (indent < column) {
                break;
            } else if (indent > column) {
                throw syntaxError("invalid indent of sequence.");
            }
            value = m2.group(2);
        }
        return seq;
    }


    private Map parseMapping(int column, String value) throws SyntaxException {
        assert Util.matches(value, REGEXP2);
        Map map = new DefaultableHashMap();
        while (true) {
            Matcher m = Util.matcher(value, REGEXP2);
            if (! m.find()) {
                throw syntaxError("mapping item is expected.");
            }
            String v = m.group(1).trim();
            Object key = toScalar(v);
            String value2 = m.group(4);
            int column2 = column + 1;

            Object elem;
            if (value2 == null || value2.length() == 0) {
                elem = parseChild(column2);
            } else {
                int valueStartColumn = column2 + m.group(1).length() + m.group(3).length();
                elem = parseValue(column2, value2, valueStartColumn);
            }
            if ("=".equals(v)) {
                setMappingDefault(map, elem);
            } else if ("<<".equals(v)) {
                mergeCollection(map, elem);
            } else {
                setMappingValueWith(map, key, elem);
            }

            String currentLine = currentLine();
            if (currentLine == null) {
                break;
            }
            Matcher m2 = Util.matcher(currentLine, REGEXP1);
            m2.find();
            int indent = m2.group(1).length();
            if (checkIndent(column, indent)) {
                break;
            }
            value = m2.group(2);
        }
        return map;
    }

    private boolean checkIndent(int column, int indent) throws SyntaxException {
        if (indent < column) {
            return true;
        } else if (indent > column) {
            throw syntaxError("invalid indent of mapping.");
        }
        return false;
    }


    private Object parseScalar(String value) {
        Object data = createScalar(toScalar(value));
        getLine();
        return data;
    }


    private Object toScalar(String value) {
        Matcher m;
        m = Util.matcher(value, "^\"(.*)\"([ \t]*#.*$)?");
        if (m.find()) {
            return m.group(1);
        }

        m = Util.matcher(value, "^'(.*)'([ \t]*#.*$)?");
        if (m.find()) {
            return m.group(1);
        }

        m = Util.matcher(value, "^(.*\\S)[ \t]*#");
        if (m.find()) {
            value = m.group(1);
        }

        if (Util.matches(value, "^-?0x\\d+$")) {
            return Integer.parseInt(value, 16);
        }

        if (Util.matches(value, "^-?0\\d+$")) {
            return Integer.parseInt(value, 8);
        }

        if (Util.matches(value, "^-?\\d+$")) {
            return Integer.parseInt(value, 10);
        }

        if (Util.matches(value, "^-?\\d+\\.\\d+$")) {
            return Double.parseDouble(value);
        }

        if (Util.matches(value, "^(true|yes|on)$")) {
            return Boolean.TRUE;
        }

        if (Util.matches(value, "^(false|no|off)$")) {
            return Boolean.FALSE;
        }

        if (Util.matches(value, "^(null|~)$")){
            return null;
        }

        if (Util.matches(value, "^:(\\w+)$"))       {
            return value;
        }

        m = Util.matcher(value, "^(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d)$");
        if (m.find()) {
            int year  = Integer.parseInt(m.group(1));
            int month = Integer.parseInt(m.group(2));
            int day   = Integer.parseInt(m.group(3));
            Calendar cal = Calendar.getInstance();
            //noinspection MagicConstant
            cal.set(year, month, day, 0, 0, 0);
            return cal.getTime();
        }

        m = Util.matcher(value, "^(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d)(?:[Tt]|[ \t]+)(\\d\\d?):(\\d\\d):(\\d\\d)(\\.\\d*)?(?:Z|[ \t]*([-+]\\d\\d?)(?::(\\d\\d))?)?$");
        if (m.find()) {
            int year    = Integer.parseInt(m.group(1));
            int month   = Integer.parseInt(m.group(2));
            int day     = Integer.parseInt(m.group(3));
            int hour    = Integer.parseInt(m.group(4));
            int min     = Integer.parseInt(m.group(5));
            int sec     = Integer.parseInt(m.group(6));

            String timezone = "GMT" + m.group(8) + ":" + m.group(9);
            Calendar cal = Calendar.getInstance();
            //noinspection MagicConstant
            cal.set(year, month, day, hour, min, sec);
            cal.setTimeZone(TimeZone.getTimeZone(timezone));
            return cal.getTime();
        }

        return value;
    }

}
