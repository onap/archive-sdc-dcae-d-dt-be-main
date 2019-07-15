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

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;

/**
 *  rule for validation.
 *  Validator class generates rule instances.
 *
 */
public class Rule implements Serializable{
    private static final String RANGE1 = "/range";
    private static final String RANGE2 = "range:";
    private static final String ENUM_CONFLICT = "enum.conflict";
    private static final String MAP_CONFLICT = "map.conflict";
    private static final String LENGTH1 = "/length";
    private static final String LENGTH2 = "length:";
    private static final String LENGTH3 = "/length/";
    private static final String SEQ_CONFLICT = "seq.conflict";
    private static final String PATTERN1 = "pattern:";
    private static final String MAPPING1 = "mapping:";
    private static final String SEQUENCE1 = "/sequence";
    private static final String MAX_EX = "max-ex";
    private static final String MIN_EX = "min-ex";
    private static final String TYPE1 = "/type";
    private static final String TYPE_NOTSTR = "type.notstr";
    private static final String TYPE_UNKNOWN = "type.unknown";
    private static final String IDENT1 = "ident:";
    private static final String UNIQUE1 = "unique:";
    private static final String MAPPING2 = "/mapping";
    private static final String MAPPING3 = "/mapping/=";
    private static final String MAPPING4 = "/mapping/";
    private static final String SEQUENCE2 = "sequence:";
    private static final String SCALAR_CONFLICT = "scalar.conflict";
    private static final String UNIQUE_NOTBOOL = "unique.notbool";
    private static final String UNIQUE_NOTSCALAR = "unique.notscalar";
    private static final String UNIQUE_ONROOT = "unique.onroot";
    private static final String UNIQUE2 = "/unique";
    private static final String IDENT_ONROOT = "ident.onroot";
    private static final String IDENT_NOTSCALAR = "ident.notscalar";
    private static final String IDENT_NOTMAP = "ident.notmap";
    private static final String MAP = "map";
    private static final String EMPTY_STRING = "";
    private static final String SLASH = "/";
    private static final String SCHEMA_NOTMAP = "schema.notmap";
    private static final String SCHEMA_NOTMAP1 = "schema.notmap: {}";
    private static final String PATTERN2 = "/pattern";
    private static final String PATTERN_NOTSTR = "pattern.notstr";
    private static final String PATTERN_NOTMATCH = "pattern.notmatch";
    private static final String REQUIRED_NOTBOOL = "required.notbool";
    private static final String REQUIRED1 = "/required";
    private static final String PATTERN_SYNTAXERR = "pattern.syntaxerr";
    private static final String PATTERN_SYNTAX_EXCEPTION = "PatternSyntaxException: {}";
    private static final String SEQUENCE_NOTSEQ = "sequence.notseq";
    private static final String SEQUENCE_NOELEM = "sequence.noelem";
    private static final String SEQUENCE_TOOMANY = "sequence.toomany";
    private static final String SEQUENCE3 = "/sequence/";
    private static final String MAPPING_NOTMAP = "mapping.notmap";
    private static final String MAPPING_NOELEM = "mapping.noelem";
    private static final String IDENT2 = "/ident";
    private static final String IDENT_NOTBOOL = "ident.notbool";
    private static final String LENGTH_MAXEXLEMINEX = "length.maxexleminex";
    private static final String LENGTH_MAXEXLEMIN = "length.maxexlemin";
    private static final String TWO_SPACES = "  ";
    private static final String NAME1 = "name:     ";
    private static final String DESC1 = "desc:     ";
    private static final String TYPE2 = "type:     ";
    private static final String REQUIRED2 = "required: ";
    private static final String PATTERN3 = "pattern:  ";
    private static final String REGEXP = "regexp:   ";
    private static final String ASSERT1 = "assert:   ";
    private static final String IDENT3 = "ident:    ";
    private static final String UNIQUE3 = "unique:   ";
    private static final String ENUM2 = "enum:\n";
    private static final String RANGE3 = "range:     { ";
    private static final String NAME_CONSTANT = "name";
    private static final String DESC_CONSTANT = "desc";
    private static final String SHORT = "short";
    private static final String REQUIRED_CONSTANT = "required";
    private static final String TYPE = "type";
    private static final String PATTERN_CONSTANT = "pattern";
    private static final String SEQUENCE_CONSTANT = "sequence";
    private static final String MAPPING = "mapping";
    private static final String ASSERT = "assert";
    private static final String RANGE_CONSTANT = "range";
    private static final String LENGTH_CONSTANT = "length";
    private static final String IDENT_CONSTANT = "ident";
    private static final String UNIQUE_CONSTANT = "unique";
    private static final String ENUM = "enum:";
    private static final String ENUM1 = "/enum";
    private static final String MAX = "max";
    private static final String MIN = "min";

    private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();

    private Rule parent;
    private String name = null;
    private String desc = null;
    private String shortValue = null; //added by jora: only used for map types
    private boolean required = false;
    private String typeValue = null;
    private Class typeClass = null;
    private String pattern = null;
    private Pattern patternRegexp = null;
    private List enumList = null;
    private List sequence = null;
    private DefaultableHashMap _mapping = null;
    private String _assert = null;
    private Map<String,Object> range = null;
    private Map<String,Integer> length = null;
    private boolean ident = false;
    private boolean unique = false;

    private static final int CODE_NAME     = NAME_CONSTANT.hashCode();
    private static final int CODE_DESC     = DESC_CONSTANT.hashCode();
    private static final int CODE_SHORT    = SHORT.hashCode();
    private static final int CODE_REQUIRED = REQUIRED_CONSTANT.hashCode();
    private static final int CODE_TYPE     = TYPE.hashCode();
    private static final int CODE_PATTERN  = PATTERN_CONSTANT.hashCode();
    private static final int CODE_LENGTH   = LENGTH_CONSTANT.hashCode();
    private static final int CODE_RANGE    = RANGE_CONSTANT.hashCode();
    private static final int CODE_ASSERT   = ASSERT.hashCode();
    private static final int CODE_IDENT    = IDENT_CONSTANT.hashCode();
    private static final int CODE_UNIQUE   = UNIQUE_CONSTANT.hashCode();
    private static final int CODE_ENUM     = ENUM.hashCode();
    private static final int CODE_MAPPING  = MAPPING.hashCode();
    private static final int CODE_SEQUENCE = SEQUENCE_CONSTANT.hashCode();

    public Rule(Object schema, Rule parent) {
        if (schema != null) {
            if (! (schema instanceof Map)) {
                throw schemaError(SCHEMA_NOTMAP, null, SLASH, null, null);
            }
            Map ruleTable = new IdentityHashMap();
            init((Map)schema, EMPTY_STRING, ruleTable);
        }
        this.parent = parent;
    }

    public Rule(Object schema) {
        this(schema, null);
    }

    public Rule(Map schema, Rule parent) {
        if (schema != null) {
            Map ruleTable = new IdentityHashMap();
            init(schema, EMPTY_STRING, ruleTable);
        }
        this.parent = parent;
    }

    public Rule(Map schema) {
        this(schema, null);
    }

    public Rule() {
        this(null, null);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getShort() { return shortValue; }
    public void setShort(String key) { shortValue = key; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public String getType() { return typeValue; }
    public void setType(String type) { this.typeValue = type; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public Pattern getPatternRegexp() { return patternRegexp; }

    public List getEnum() {
    	return enumList;
    }
    public void setEnum(List enumList) { this.enumList = enumList; }

    public List getSequence() { return sequence; }
    public void setSequence(List sequence) { this.sequence = sequence; }

    public DefaultableHashMap getMapping() { return _mapping; }
    public void setMapping(DefaultableHashMap mapping) { _mapping = mapping; }

    public String getAssert() { return _assert; }
    public void setAssert(String assertString) { _assert = assertString; }

    public Map getRange() { return range; }
    public void setRange(Map range) { this.range = range; }

    public Map getLength() { return length; }
    public void setLength(Map length) { this.length = length; }

    public boolean isIdent() { return ident; }

    public boolean isUnique() { return unique; }
    public void setUnique(boolean unique) { this.unique = unique; }

    private static SchemaException schemaError(String errorSymbol, Rule rule, String path, Object value, Object[] args) {
        String msg = Messages.buildMessage(errorSymbol, value, args);
        return new SchemaException(msg, path, value, rule);
    }

    private void init(Object elem, String path, Map ruleTable) {
        assert elem != null;
        if (! (elem instanceof Map)) {
            if (path == null || path.isEmpty()) {
                path = SLASH;
            }
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), SCHEMA_NOTMAP1, elem);
            throw schemaError(SCHEMA_NOTMAP, null, path, null, null);
        }
        init((Map)elem, path, ruleTable);
    }

    private void init(Map hash, String path, Map ruleTable) {
        Rule rule = this;
        ruleTable.put(hash, rule);

        // 'type:' entry
        Object type = hash.get(TYPE);
        initTypeValue(type, rule, path);

        // other entries
        for (Iterator it = hash.keySet().iterator(); it.hasNext(); ) {
            Object key = it.next();
            Object value = hash.get(key);
            int code = key.hashCode();

            if (code == CODE_TYPE && key.equals(TYPE)) {
                // done
            } else if (code == CODE_NAME && key.equals(NAME_CONSTANT)) {
                initNameValue(value);
            } else if (code == CODE_DESC && key.equals(DESC_CONSTANT)) {
                initDescValue(value);
            } else if (code == CODE_SHORT && key.equals(SHORT)) {
                initShortValue(value, rule, path);
            } else if (code == CODE_REQUIRED && key.equals(REQUIRED_CONSTANT)) {
                initRequiredValue(value, rule, path);
            } else if (code == CODE_PATTERN && key.equals(PATTERN_CONSTANT)) {
                initPatternValue(value, rule, path);
            } else if (code == CODE_ENUM && key.equals(ENUM)) {
                initEnumValue(value, rule, path);
            } else if (code == CODE_ASSERT && key.equals(ASSERT)) {
                initAssertValue(value, rule, path);
            } else if (code == CODE_RANGE && key.equals(RANGE_CONSTANT)) {
                initRangeValue(value, rule, path);
            } else if (code == CODE_LENGTH && key.equals(LENGTH_CONSTANT)) {
                initLengthValue(value, rule, path);
            } else if (code == CODE_IDENT && key.equals(IDENT_CONSTANT)) {
                initIdentValue(value, rule, path);
            } else if (code == CODE_UNIQUE && key.equals(UNIQUE_CONSTANT)) {
                initUniqueValue(value, rule, path);
            } else if (code == CODE_SEQUENCE && key.equals(SEQUENCE_CONSTANT)) {
                rule = initSequenceValue(value, rule, path, ruleTable);
            } else if (code == CODE_MAPPING && key.equals(MAPPING)) {
                rule = initMappingValue(value, rule, path, ruleTable);
            }
        }

        checkConfliction(hash, rule, path);
    }

    private void initTypeValue(Object value, Rule rule, String path) {
        if (value == null) {
            value = Types.getDefaultType();
        }
        if (! (value instanceof String)) {
            throw schemaError(TYPE_NOTSTR, rule, path + TYPE1, typeValue, null);
        }
        typeValue = (String)value;
        typeClass = Types.typeClass(typeValue);
        if (! Types.isBuiltinType(typeValue)) {
            throw schemaError(TYPE_UNKNOWN, rule, path + TYPE1, typeValue, null);
        }
    }


    private void initNameValue(Object value) {
        name = value.toString();
    }


    private void initDescValue(Object value) {
        desc = value.toString();
    }

    private void initShortValue(Object value, Rule rule, String path) {

        //the short form specification is to be interpreted as key if the type is a map or as an
        //index if the target is a sequence (as index 0 actually)  
        if (!Types.isCollectionType(typeValue)) {
            throw schemaError("range.notcollection", rule, path + "/short", value, null);
        }
        //we should also verify that it points to a declared key of the mapping .. not really, as it would
                //fail the overall grammar
        shortValue = value.toString();
    }

    private void initRequiredValue(Object value, Rule rule, String path) {
        if (! (value instanceof Boolean)) {
            throw schemaError(REQUIRED_NOTBOOL, rule, path + REQUIRED1, value, null);
        }
        required = (Boolean) value;
    }


    private void initPatternValue(Object value, Rule rule, String path) {
        if (! (value instanceof String)) {
            throw schemaError(PATTERN_NOTSTR, rule, path + PATTERN2, value, null);
        }
        pattern = (String)value;
        Matcher m = Util.matcher(pattern, "\\A/(.*)/([mi]?[mi]?)\\z");
        if (! m.find()) {
            throw schemaError(PATTERN_NOTMATCH, rule, path + PATTERN2, value, null);
        }
        String pat = m.group(1);
        String opt = m.group(2);
        int flag = 0;
        if (opt.indexOf('i') >= 0) {
            flag += Pattern.CASE_INSENSITIVE;
        }
        if (opt.indexOf('m') >= 0) {
            flag += Pattern.DOTALL;   // not MULTILINE
        }
        try {
            patternRegexp = Pattern.compile(pat, flag);
        } catch (PatternSyntaxException ex) {
            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), PATTERN_SYNTAX_EXCEPTION, ex);
            throw schemaError(PATTERN_SYNTAXERR, rule, path + PATTERN2, value, null);
        }
    }


    private void initEnumValue(Object value, Rule rule, String path) {
        if (! (value instanceof List)) {
            throw schemaError("enum.notseq", rule, path + ENUM1, value, null);
        }
        enumList = (List)value;
        if (Types.isCollectionType(typeValue)) {
            throw schemaError("enum.notscalar", rule, path, ENUM, null);
        }
        Map elemTable = new HashMap();
        for (Iterator it = enumList.iterator(); it.hasNext(); ) {
            Object elem = it.next();
            if (! Util.isInstanceOf(elem, typeClass)) {
                throw schemaError("enum.type.unmatch", rule, path + ENUM1, elem, new Object[] { Types.typeName(typeValue) });
            }
            if (elemTable.containsKey(elem)) {
                throw schemaError("enum.duplicate", rule, path + ENUM1, elem, null);
            }
            elemTable.put(elem, Boolean.TRUE);
        }
    }


    private void initAssertValue(Object value, Rule rule, String path) {
        if (! (value instanceof String)) {
            throw schemaError("assert.notstr", rule, path + "/assert", value, null);
        }
        _assert = (String)value;
        if (! Util.matches(_assert, "\\bval\\b")) {
            throw schemaError("assert.noval", rule, path + "/assert", value, null);
        }
    }


    private void initRangeValue(Object value, Rule rule, String path) {
        if (! (value instanceof Map)) {
            throw schemaError("range.notmap", rule, path + RANGE1, value, null);
        }
        if (Types.isCollectionType(typeValue) || "bool".equals(typeValue)) {
            throw schemaError("range.notscalar", rule, path, RANGE2, null);
        }
        range = (Map)value;
        for (Iterator it = range.keySet().iterator(); it.hasNext(); ) {
            Object rkey = it.next();
            Object rval = range.get(rkey);
            if (MAX.equals(rkey) || MIN.equals(rkey) || rkey.equals(MAX_EX) || rkey.equals(MIN_EX)) {
                if (! Util.isInstanceOf(rval, typeClass)) {
                    String typename = Types.typeName(typeValue);
                    throw schemaError("range.type.unmatch", rule, path + "/range/" + rkey, rval, new Object[] { typename });
                }
            } else {
                throw schemaError("range.undefined", rule, path + "/range/" + rkey, rkey.toString() + ":", null);
            }
        }
        if (range.containsKey(MAX) && range.containsKey(MAX_EX)) {
            throw schemaError("range.twomax", rule, path + RANGE1, null, null);
        }
        if (range.containsKey(MIN) && range.containsKey(MIN_EX)) {
            throw schemaError("range.twomin", rule, path + RANGE1, null, null);
        }
        //
        Object max    = range.get(MAX);
        Object min    = range.get(MIN);
        Object maxEx = range.get(MAX_EX);
        Object minEx = range.get(MIN_EX);
        Object[] args;

        if (max != null) {
            if (min != null && Util.compareValues(max, min) < 0) {
                args = new Object[] { max, min };
                throw schemaError("range.maxltmin", rule, path + RANGE1, null, args);
            } else if (minEx != null && Util.compareValues(max, minEx) <= 0) {
                args = new Object[] { max, minEx };
                throw schemaError("range.maxleminex", rule, path + RANGE1, null, args);
            }
        } else if (maxEx != null) {
            if (min != null && Util.compareValues(maxEx, min) <= 0) {
                args = new Object[] { maxEx, min };
                throw schemaError("range.maxexlemin", rule, path + RANGE1, null, args);
            } else if (minEx != null && Util.compareValues(maxEx, minEx) <= 0) {
                args = new Object[] { maxEx, minEx };
                throw schemaError("range.maxexleminex", rule, path + RANGE1, null, args);
            }
        }
    }


    private void initLengthValue(Object value, Rule rule, String path) {
        if (! (value instanceof Map)) {
            throw schemaError("length.notmap", rule, path + LENGTH1, value, null);
        }
        length = (Map)value;
        if (! ("str".equals(typeValue) || "text".equals(typeValue))) {
            throw schemaError("length.nottext", rule, path, LENGTH2, null);
        }
        for (String k : length.keySet()) {
            Integer v = length.get(k);
            if (MAX.equals(k) || MIN.equals(k) || k.equals(MAX_EX) || k.equals(MIN_EX)) {
                if (v != null) {
                    throw schemaError("length.notint", rule, path + LENGTH3 + k, v, null);
                }
            } else {
                throw schemaError("length.undefined", rule, path + LENGTH3 + k, k + ":", null);
            }
        }
        if (length.containsKey(MAX) && length.containsKey(MAX_EX)) {
            throw schemaError("length.twomax", rule, path + LENGTH1, null, null);
        }
        if (length.containsKey(MIN) && length.containsKey(MIN_EX)) {
            throw schemaError("length.twomin", rule, path + LENGTH1, null, null);
        }
       
        Integer max    =  length.get(MAX);
        Integer min    =  length.get(MIN);
        Integer maxEx = length.get(MAX_EX);
        Integer minEx = length.get(MIN_EX);
        Object[] args;

        if (max != null) {
            if (min != null && max.compareTo(min) < 0) {
                args = new Object[] { max, min };
                throw schemaError("length.maxltmin", rule, path + LENGTH1, null, args);
            } else if (minEx != null && max.compareTo(minEx) <= 0) {
                args = new Object[] { max, minEx };
                throw schemaError("length.maxleminex", rule, path + LENGTH1, null, args);
            }
        } else if (maxEx != null) {
            if (min != null && maxEx.compareTo(min) <= 0) {
                args = new Object[] { maxEx, min };
                throw schemaError(LENGTH_MAXEXLEMIN, rule, path + LENGTH1, null, args);
            } else if (minEx != null && maxEx.compareTo(minEx) <= 0) {
                args = new Object[] { maxEx, minEx };
                throw schemaError(LENGTH_MAXEXLEMINEX, rule, path + LENGTH1, null, args);
            }
        }
    }

    private void initIdentValue(Object value, Rule rule, String path) {
        if (value == null || ! (value instanceof Boolean)) {
            throw schemaError(IDENT_NOTBOOL, rule, path + IDENT2, value, null);
        }
        ident = (Boolean) value;
        required = true;
        if (Types.isCollectionType(typeValue)) {
            throw schemaError(IDENT_NOTSCALAR, rule, path, IDENT1, null);
        }
        if (EMPTY_STRING.equals(path)) {
            throw schemaError(IDENT_ONROOT, rule, SLASH, IDENT1, null);
        }
        if (parent == null || ! MAP.equals(parent.getType())) {
            throw schemaError(IDENT_NOTMAP, rule, path, IDENT1, null);
        }
    }


    private void initUniqueValue(Object value, Rule rule, String path) {
        if (! (value instanceof Boolean)) {
            throw schemaError(UNIQUE_NOTBOOL, rule, path + UNIQUE2, value, null);
        }
        unique = (Boolean) value;
        if (Types.isCollectionType(typeValue)) {
            throw schemaError(UNIQUE_NOTSCALAR, rule, path, UNIQUE1, null);
        }
        if (path.equals(EMPTY_STRING)) {
            throw schemaError(UNIQUE_ONROOT, rule, SLASH, UNIQUE1, null);
        }
    }


    private Rule initSequenceValue(Object value, Rule rule, String path, Map ruleTable) {
        if (value != null && ! (value instanceof List)) {
            throw schemaError(SEQUENCE_NOTSEQ, rule, path + SEQUENCE1, value.toString(), null);
        }
        sequence = (List)value;
        if (sequence == null || sequence.isEmpty()) {
            throw schemaError(SEQUENCE_NOELEM, rule, path + SEQUENCE1, value, null);
        }
        if (sequence.size() > 1) {
            throw schemaError(SEQUENCE_TOOMANY, rule, path + SEQUENCE1, value, null);
        }
        Object elem = sequence.get(0);
        if (elem == null) {
            elem = new HashMap();
        }
        int i = 0;
        rule = (Rule)ruleTable.get(elem);
        if (rule == null) {
            rule = new Rule(null, this);
            rule.init(elem, path + SEQUENCE3 + i, ruleTable);
        }
        sequence = new ArrayList();
        sequence.add(rule);
        return rule;
    }


    private Rule initMappingValue(Object value, Rule rule, String path, Map ruleTable) {
        // error check
        if (value != null && !(value instanceof Map)) {
            throw schemaError(MAPPING_NOTMAP, rule, path + MAPPING2, value.toString(), null);
        }
        Object defaultValue = null;
        if (value instanceof Defaultable) {
            defaultValue = ((Defaultable)value).getDefault();
        }
        if (value == null || ((Map)value).size() == 0 && defaultValue == null) {
            throw schemaError(MAPPING_NOELEM, rule, path + MAPPING2, value, null);
        }
        // create hash of rule
        _mapping = new DefaultableHashMap();

        if (defaultValue != null) {
            rule = (Rule)ruleTable.get(defaultValue);
            if (rule == null) {
                rule = new Rule(null, this);
                rule.init(defaultValue, path + MAPPING3, ruleTable);
            }
            _mapping.setDefault(rule);
        }

        // put rules into _mapping
        rule = putRulesIntoMap((Map) value, rule, path, ruleTable);
        return rule;
    }

    private Rule putRulesIntoMap(Map value, Rule rule, String path, Map ruleTable) {
        Map map = value;
        for (Object k : map.keySet()) {
            Object v = map.get(k);
            if (v == null) {
                v = new DefaultableHashMap();
            }
            rule = (Rule) ruleTable.get(v);
            if (rule == null) {
                rule = new Rule(null, this);
                rule.init(v, path + MAPPING4 + k, ruleTable);
            }
            if ("=".equals(k)) {
                _mapping.setDefault(rule);
            } else {
                _mapping.put(k, rule);
            }
        }
        return rule;
    }


    private void checkConfliction(Map hash, Rule rule, String path) {
        if ("seq".equals(typeValue)) {
            if (! hash.containsKey(SEQUENCE_CONSTANT)) {
                throw schemaError("seq.nosequence", rule, path, null, null);
            }
            if (enumList != null) {
                throw schemaError(SEQ_CONFLICT, rule, path, ENUM,    null);
            }
            if (pattern != null) {
                throw schemaError(SEQ_CONFLICT, rule, path, PATTERN1, null);
            }
            if (_mapping != null) {
                throw schemaError(SEQ_CONFLICT, rule, path, MAPPING1, null);
            }
            if (range != null) {
                throw schemaError(SEQ_CONFLICT, rule, path, RANGE2,   null);
            }
            if (length != null) {
                throw schemaError(SEQ_CONFLICT, rule, path, LENGTH2,  null);
            }
        } else if (typeValue.equals(MAP)) {
            if (! hash.containsKey(MAPPING)) {
                throw schemaError("map.nomapping", rule, path, null, null);
            }
            if (enumList != null) {
                throw schemaError(MAP_CONFLICT, rule, path, ENUM,     null);
            }
            if (pattern != null) {
                throw schemaError(MAP_CONFLICT, rule, path, PATTERN1,  null);
            }
            if (sequence != null) {
                throw schemaError(MAP_CONFLICT, rule, path, SEQUENCE2, null);
            }
            if (range != null) {
                throw schemaError(MAP_CONFLICT, rule, path, RANGE2,    null);
            }
            if (length != null) {
                throw schemaError(MAP_CONFLICT, rule, path, LENGTH2,   null);
            }
        } else {
            if (sequence != null) {
                throw schemaError(SCALAR_CONFLICT, rule, path, SEQUENCE2, null);
            }
            if (_mapping  != null) {
                throw schemaError(SCALAR_CONFLICT, rule, path, MAPPING1,  null);
            }
            if (enumList != null) {
                if (range != null) {
                    throw schemaError(ENUM_CONFLICT, rule, path, RANGE2,   null);
                }
                if (length != null) {
                    throw schemaError(ENUM_CONFLICT, rule, path, LENGTH2,  null);
                }
                if (pattern != null) {
                    throw schemaError(ENUM_CONFLICT, rule, path, PATTERN1, null);
                }
            }
        }
    }

    public String inspect() {
        StringBuilder sb = new StringBuilder();
        int level = 0;
        Map done = new IdentityHashMap();
        inspect(sb, level, done);
        return sb.toString();
    }

    private void inspect(StringBuilder sb, int level, Map done) {
        done.put(this, Boolean.TRUE);
        String indent = Util.repeatString(TWO_SPACES, level);
        if (name != null) {
            sb.append(indent).append(NAME1).append(name).append("\n");
        }
        if (desc != null) {
            sb.append(indent).append(DESC1).append(desc).append("\n");
        }
        if (typeValue != null) {
            sb.append(indent).append(TYPE2).append(typeValue).append("\n");
        }
        if (required) {
            sb.append(indent).append(REQUIRED2).append(required).append("\n");
        }
        if (pattern != null) {
            sb.append(indent).append(PATTERN3).append(pattern).append("\n");
        }
        if (patternRegexp != null) {
            sb.append(indent).append(REGEXP).append(patternRegexp).append("\n");
        }
        if (_assert != null) {
            sb.append(indent).append(ASSERT1).append(_assert).append("\n");
        }
        if (ident) {
            sb.append(indent).append(IDENT3).append(ident).append("\n");
        }
        if (unique) {
            sb.append(indent).append(UNIQUE3).append(unique).append("\n");
        }
        if (enumList != null) {
            appendEnums(sb, indent);
        }
        if (range != null) {
            appendRange(sb, indent);
        }
        if (sequence != null) {
            appendSequence(sb, level, done, indent);
        }
        if (_mapping != null) {
            appendMapping(sb, level, done, indent);
        }
    }

    private void appendEnums(StringBuilder sb, String indent) {
        sb.append(indent).append(ENUM2);
        for (Object anEnumList : enumList) {
            sb.append(indent).append("  - ").append(anEnumList.toString()).append("\n");
        }
    }

    private void appendMapping(StringBuilder sb, int level, Map done, String indent) {
        for (Object o : _mapping.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            Object key = entry.getKey();
            Rule rule = (Rule) entry.getValue();
            sb.append(indent).append("  ").append(Util.inspect(key));
            if (done.containsKey(rule)) {
                sb.append(": ...\n");
            } else {
                sb.append(":\n");
                rule.inspect(sb, level + 2, done);
            }
        }
    }

    private void appendSequence(StringBuilder sb, int level, Map done, String indent) {
        for (Object aSequence : sequence) {
            Rule rule = (Rule) aSequence;
            if (done.containsKey(rule)) {
                sb.append(indent).append("  ").append("- ...\n");
            } else {
                sb.append(indent).append("  ").append("- \n");
                rule.inspect(sb, level + 2, done);
            }
        }
    }

    private void appendRange(StringBuilder sb, String indent) {
        sb.append(indent).append(RANGE3);
        String[] keys = new String[] {MAX, MAX_EX, MIN, MIN_EX, };
        String colon = EMPTY_STRING;
        for (String key : keys) {
            Object val = range.get(key);
            if (val != null) {
                sb.append(colon).append(key).append(": ").append(val);
                colon = ", ";
            }
        }
        sb.append(" }\n");
    }
}
