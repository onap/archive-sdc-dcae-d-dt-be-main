/*
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.common.onaplog.Enums.LogLevel;

import java.util.Map;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

/**
 *  meta validator to validate schema definition
 */
public class MetaValidator extends Validator {

    private static final String RANGE = "range";
    private static final String MAX_EX = "max-ex";
    private static final String MIN_EX = "min-ex";
    private static final String LENGTH = "length";
    private static final String SEQUENCE = "sequence";
    private static final String ENUM_CONFLICT = "enum.conflict";
    private static final String SCALAR_CONFLICT = "scalar.conflict";
    private static final String IDENT = "ident";
    private static final String IDENT1 = "ident:";
    private static final String MAPPING = "mapping";
    private static final String PATTERN = "pattern:";
    private static final String PATTERN1 = "pattern";
    private static final String TYPE_MAP = "    type:      map\n";
    private static final String TYPE_STR = "    type:      str\n";
    private static final String TYPE_BOOL = "    type:      bool\n";
    private static final String MAPPING1 = "    mapping:\n";
    private static final String TYPE_SCALAR = "        type:     scalar\n";
    private static final String TYPE_INT = "        type:     int\n";

    private static OnapLoggerError errLogger = OnapLoggerError.getInstance();


    private static final String META_SCHEMA = new StringBuilder().
            append("").
            append("name:      MAIN\n").
            append("type:      map\n").
            append("required:  yes\n").
            append("mapping:   &main-rule\n").
            append(" \"name\":\n").
            append(TYPE_STR).
            append(" \"desc\":\n").
            append(TYPE_STR).
            append(" \"type\":\n").
            append(TYPE_STR).
            append("    #required:  yes\n").
            append("    enum:\n").
            append("      - seq\n").
            append("      #- sequence\n").
            append("      #- list\n").
            append("      - map\n").
            append("      #- mapping\n").
            append("      #- hash\n").
            append("      - str\n").
            append("      #- string\n").
            append("      - int\n").
            append("      #- integer\n").
            append("      - float\n").
            append("      - number\n").
            append("      #- numeric\n").
            append("      - bool\n").
            append("      #- boolean\n").
            append("      - text\n").
            append("      - date\n").
            append("      - time\n").
            append("      - timestamp\n").
            append("      #- object\n").
            append("      - any\n").
            append("      - scalar\n").
            append("      #- collection\n").
            append(" \"required\":\n").
            append(TYPE_BOOL).
            append(" \"enum\":\n").
            append("    type:      seq\n").
            append("    sequence:\n").
            append("      - type:     scalar\n").
            append("        unique:   yes\n").
            append(" \"pattern\":\n").
            append(TYPE_STR).
            append(" \"assert\":\n").
            append(TYPE_STR).
            append("    pattern:   /\\bval\\b/\n").
            append(" \"range\":\n").
            append(TYPE_MAP).
            append(MAPPING1).
            append("     \"max\":\n").
            append(TYPE_SCALAR).
            append("     \"min\":\n").
            append(TYPE_SCALAR).
            append("     \"max-ex\":\n").
            append(TYPE_SCALAR).
            append("     \"min-ex\":\n").
            append(TYPE_SCALAR).
            append(" \"length\":\n").
            append(TYPE_MAP).
            append(MAPPING1).
            append("     \"max\":\n").
            append(TYPE_INT).
            append("     \"min\":\n").
            append(TYPE_INT).
            append("     \"max-ex\":\n").
            append(TYPE_INT).
            append("     \"min-ex\":\n").
            append(TYPE_INT).
            append(" \"ident\":\n").
            append(TYPE_BOOL).
            append(" \"unique\":\n").
            append(TYPE_BOOL).
            append(" \"sequence\":\n").
            append("    name:      SEQUENCE\n").
            append("    type:      seq\n").
            append("    sequence:\n").
            append("      - type:      map\n").
            append("        mapping:   *main-rule\n").
            append("        name:      MAIN\n").
            append("        #required:  yes\n").
            append(" \"mapping\":\n").
            append("    name:      MAPPING\n").
            append(TYPE_MAP).
            append(MAPPING1).
            append("      =:\n").
            append("        type:      map\n").
            append("        mapping:   *main-rule\n").
            append("        name:      MAIN\n").
            append("        #required:  yes\n").
            toString();

    /**
     *
     * ex.
     * <pre>
     *  MetaValidator meta_validator = MetaValidator();
     *  Map schema = YamlUtil.loadFile("schema.yaml");
     *  List errors = meta_validator.validate(schema);
     *  if (errors != null && errors.size() > 0) {
     *    for (Iterator it = errors.iterator(); it.hasNext(); ) {
     *      ValidationException error = (ValidationException)it.next();
     *      System.err.println(" - [" + error.getPath() + "] " + error.getMessage());
     *    }
     *  }
     * </pre>
     */

    private static Validator __instance;

    private MetaValidator(Map schema) {
        super(schema);
    }


    public static Validator instance() {
        synchronized (MetaValidator.class) {
            if (__instance == null) {
                try {
                    Map schema = (Map) YamlUtil.load(META_SCHEMA);
                    __instance = new MetaValidator(schema);
                } catch (SyntaxException ex) {
                    errLogger.log(LogLevel.INFO,"MetaValidator","Failed validating schema: {}",ex);
                    assert false;
                }
            }
        }

        return __instance;
    }

    @Override
    public void postValidationHook(Object value, Rule rule, ValidationContext theContext) {
        if (value == null) {
            return;   // really?
        }
        if (! "MAIN".equals(rule.getName())) {
            return;
        }
        assert value instanceof Map;
        Map map = (Map)value;
        String type = (String)map.get("type");
        if (type == null) {
            type = Types.getDefaultType();
        }

        if (map.containsKey(PATTERN1)) {
            String pattern = (String)map.get(PATTERN1);
            Matcher m = Util.matcher(pattern, "\\A\\/(.*)\\/([mi]?[mi]?)\\z");
            String pat = m.find() ? m.group(1) : pattern;
            try {
                Pattern.compile(pat);
            } catch (PatternSyntaxException ex) {
                errLogger.log(LogLevel.INFO,"MetaValidator","pattern.syntaxerr: {}",ex);
                theContext.addError("pattern.syntaxerr", rule, PATTERN1, pattern, null);
            }
        }
        if (map.containsKey("enum")) {
            List enumList = (List)map.get("enum");
            if (Types.isCollectionType(type)) {
                theContext.addError("enum.notscalar", rule, "enum:", (Object[])null);
            } else {
                checkEnum(rule, theContext, type, enumList);
            }
        }
        if (map.containsKey("assert")) {
            errLogger.log(LogLevel.ERROR, this.getClass().getName(), "*** warning: sorry, 'assert:' is not supported in current version of Kwalify-java.");

        }

        if (map.containsKey(RANGE)) {
            Map range = (Map)map.get(RANGE);

            if (Types.isCollectionType(type) || "bool".equals(type) || "any".equals(type)) {
                theContext.addError("range.notscalar", rule, "range:", null, null);
            } else {
                rangeCheck(rule, theContext, type, range);
            }
            if (range.containsKey("max") && range.containsKey(MAX_EX)) {
                theContext.addError("range.twomax", rule, RANGE, null, null);
            }
            if (range.containsKey("min") && range.containsKey(MIN_EX)) {
                theContext.addError("range.twomin", rule, RANGE, null, null);
            }
            Object max    = range.get("max");
            Object min    = range.get("min");
            Object maxEx = range.get(MAX_EX);
            Object minEx = range.get(MIN_EX);
            Object[] args;
            if (max != null) {
                if (min != null && Util.compareValues(max, min) < 0) {
                    args = new Object[] { max, min };
                    theContext.addError("range.maxltmin", rule, RANGE, null, args);
                } else if (minEx != null && Util.compareValues(max, minEx) <= 0) {
                    args = new Object[] { max, minEx };
                    theContext.addError("range.maxleminex", rule, RANGE, null, args);
                }
            } else if (maxEx != null) {
                if (min != null && Util.compareValues(maxEx, min) <= 0) {
                    args = new Object[] { maxEx, min };
                    theContext.addError("range.maxexlemin", rule, RANGE, null, args);
                } else if (minEx != null && Util.compareValues(maxEx, minEx) <= 0) {
                    args = new Object[] { maxEx, minEx };
                    theContext.addError("range.maxexleminex", rule, RANGE, null, args);
                }
            }
        }
        if (map.containsKey(LENGTH)) {
            Map length = (Map)map.get(LENGTH);

            if (! ("str".equals(type) || "text".equals(type))) {
                theContext.addError("length.nottext", rule, "length:", (Object[])null);
            }

            if (length.containsKey("max") && length.containsKey(MAX_EX)) {
                theContext.addError("length.twomax", rule, LENGTH, (Object[])null);
            }
            if (length.containsKey("min") && length.containsKey(MIN_EX)) {
                theContext.addError("length.twomin", rule, LENGTH, (Object[])null);
            }
            Integer max    = (Integer)length.get("max");
            Integer min    = (Integer)length.get("min");
            Integer maxEx = (Integer)length.get(MAX_EX);
            Integer minEx = (Integer)length.get(MIN_EX);
            Object[] args;
            if (max != null) {
                if (min != null && max.compareTo(min) < 0) {
                    args = new Object[] { max, min };
                    theContext.addError("length.maxltmin", rule, LENGTH, null, args);
                } else if (minEx != null && max.compareTo(minEx) <= 0) {
                    args = new Object[] { max, minEx };
                    theContext.addError("length.maxleminex", rule, LENGTH, null, args);
                }
            } else if (maxEx != null) {
                if (min != null && maxEx.compareTo(min) <= 0) {
                    args = new Object[] { maxEx, min };
                    theContext.addError("length.maxexlemin", rule, LENGTH, null, args);
                } else if (minEx != null && maxEx.compareTo(minEx) <= 0) {
                    args = new Object[] { maxEx, minEx };
                    theContext.addError("length.maxexleminex", rule, LENGTH, null, args);
                }
            }
        }

        if (map.containsKey("unique")) {
            Boolean unique = (Boolean)map.get("unique");
            if (unique && Types.isCollectionType(type)) {
                theContext.addError("unique.notscalar", rule, "unique:", (Object[])null);
            }
            if (theContext.getPath().length() == 0) {
                theContext.addError("unique.onroot", rule, "", "unique:", null);
            }
        }

        if (map.containsKey(IDENT)) {
            Boolean ident = (Boolean)map.get(IDENT);
            if (ident && Types.isCollectionType(type)) {
                theContext.addError("ident.notscalar", rule, IDENT1, (Object[])null);
            }
            if (theContext.getPath().length() == 0) {
                theContext.addError("ident.onroot", rule, "/", IDENT1, null);
            }
        }

        if (map.containsKey(SEQUENCE)) {
            List seq = (List)map.get(SEQUENCE);

            if (seq == null || seq.isEmpty()) {
                theContext.addError("sequence.noelem", rule, SEQUENCE, seq, null);
            } else if (seq.size() > 1) {
                theContext.addError("sequence.toomany", rule, SEQUENCE, seq, null);
            } else {
                Object item = seq.get(0);
                assert item instanceof Map;
                Map m = (Map)item;
                Boolean ident2 = (Boolean)m.get(IDENT);
                if (ident2 != null && ident2 && ! "map".equals(m.get("type"))) {
                    theContext.addError("ident.notmap", null, "sequence/0", IDENT1, null);
                }
            }
        }
        if (map.containsKey(MAPPING)) {
            Map mapping = (Map)map.get(MAPPING);

            Object defaultValue = null;
            if (mapping != null && mapping instanceof Defaultable) {
                defaultValue = ((Defaultable)mapping).getDefault();
            }
            if (mapping == null || (mapping.size() == 0 && defaultValue == null)) {
                theContext.addError("mapping.noelem", rule, MAPPING, mapping, null);
            }
        }
        if ("seq".equals(type)) {
            if (! map.containsKey(SEQUENCE)) {
                theContext.addError("seq.nosequence", rule, null, (Object[])null);
            }
            if (map.containsKey(PATTERN1)) {
                theContext.addError("seq.conflict", rule, PATTERN, (Object[])null);
            }
            if (map.containsKey(MAPPING)) {
                theContext.addError("seq.conflict", rule, "mapping:", (Object[])null);
            }
        } else if ("map".equals(type)) {
            if (! map.containsKey(MAPPING)) {
                theContext.addError("map.nomapping", rule, null, (Object[])null);
            }
            if (map.containsKey(PATTERN1)) {
                theContext.addError("map.conflict", rule, PATTERN, (Object[])null);
            }
            if (map.containsKey(SEQUENCE)) {
                theContext.addError("map.conflict", rule, "sequence:", (Object[])null);
            }
        } else {
            if (map.containsKey(SEQUENCE)) {
                theContext.addError(SCALAR_CONFLICT, rule, "sequence:", (Object[])null);
            }
            if (map.containsKey(MAPPING)) {
                theContext.addError(SCALAR_CONFLICT, rule, "mapping:", (Object[])null);
            }
            if (map.containsKey("enum")) {
                if (map.containsKey(RANGE)) {
                    theContext.addError(ENUM_CONFLICT, rule, "range:", (Object[])null);
                }
                if (map.containsKey(LENGTH)) {
                    theContext.addError(ENUM_CONFLICT, rule, "length:", (Object[])null);
                }
                if (map.containsKey(PATTERN1)) {
                    theContext.addError(ENUM_CONFLICT, rule, PATTERN, (Object[])null);
                }
            }
        }
    }

    private void checkEnum(Rule rule, ValidationContext theContext, String type, List enumList) {
        for (Object elem : enumList) {
            if (!Types.isCorrectType(elem, type)) {
                theContext.addError("enum.type.unmatch", rule, "enum", elem, new Object[]{Types.typeName(type)});
            }
        }
    }

    private void rangeCheck(Rule rule, ValidationContext theContext, String type, Map range) {
        for (Object o : range.keySet()) {
            String k = (String) o;
            Object v = range.get(k);
            if (!Types.isCorrectType(v, type)) {
                theContext.addError("range.type.unmatch", rule, "range/" + k, v, new Object[]{Types.typeName(type)});
            }
        }
    }

}
