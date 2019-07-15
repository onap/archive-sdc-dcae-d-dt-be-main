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

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Date;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class Util {
    private static final int VALUE_INTEGER =  1;
    private static final int VALUE_DOUBLE  =  2;
    private static final int VALUE_STRING  =  4;
    private static final int VALUE_BOOLEAN =  8;
    private static final int VALUE_DATE    = 16;
    private static final int VALUE_OBJECT  = 32;
    private static HashMap<String,Pattern> __patterns = new HashMap<>();

    private Util(){
        // You shouldn't instantiate this class
    }

    /**
     *  inspect List or Map
     */
    public static String inspect(Object obj) {
        StringBuilder sb = new StringBuilder();
        inspect(obj, sb, null);
        return sb.toString();
    }

    private static void inspect(Object obj, StringBuilder sb, IdentityHashMap done) {
        if (obj == null) {
            sb.append("nil");   // null?
        } else if (obj instanceof String) {
            inspect((String)obj, sb);
        } else if (obj instanceof IdentityHashMap) {
            if (done == null) {
                done = new IdentityHashMap();
            }
            if (done.containsKey(obj)) {
                sb.append("{...}");
            } else {
                done.put(obj, Boolean.TRUE);
                inspect((Map)obj, sb, done);
            }
        } else if (obj instanceof List) {
            if (done == null) {
                done = new IdentityHashMap();
            }
            if (done.containsKey(obj)) {
                sb.append("[...]");
            } else {
                done.put(obj, Boolean.TRUE);
                inspect((List)obj, sb);
            }
        } else {
            sb.append(obj.toString());
        }
    }

    private static void inspect(Map map, StringBuilder sb, IdentityHashMap done) {
        sb.append('{');
        List list = new ArrayList(map.keySet());
        Collections.sort(list);
        int i = 0;
        for (Iterator it = list.iterator(); it.hasNext(); i++) {
            Object key   = it.next();
            Object value = map.get(key);
            if (i > 0) {
                sb.append(", ");
            }
            inspect(key, sb, done);
            sb.append("=>");
            inspect(value, sb, done);
        }
        sb.append('}');
    }

    private static void inspect(List list, StringBuilder sb) {
        sb.append('[');
        int i = 0;
        for (Iterator it = list.iterator(); it.hasNext(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Object item = it.next();
            inspect(item, sb, null);
        }
        sb.append(']');
    }

    private static void inspect(String str, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            switch (ch) {
              case '"':
                  sb.append("\\\"");
                  break;
              case '\n':
                  sb.append("\\n");
                  break;
              case '\r':
                  sb.append("\\r");
                  break;
              case '\t':
                  sb.append("\\t");
                  break;
              default:
                  sb.append(ch);
                  break;
            }
        }
        sb.append('"');
    }

    /**
     *  match pattern and return Mather object.
     *
     *  ex.
     *  <pre>
     *   String target = " name = foo\n mail = foo@mail.com\m";
     *   Matcher m = Util.matcher(target, "^\\s*(\\w+)\\s*=\\s*(.*)$");
     *   while (m.find()) {
     *     String key   = m.group(1);
     *     String value = m.gropu(2);
     *   }
     *  </pre>
     */
    public static Matcher matcher(String target, String regexp) {
        Pattern pat = __patterns.get(regexp);
        if (pat == null) {
            pat = Pattern.compile(regexp);
            __patterns.put(regexp, pat);
        }
        return pat.matcher(target);
    }

    /**
     *  return if pattern matched or not.
     *
     *  ex.
     *  <pre>
     *   String target = " name = foo\n";
     *   if (Util.matches(target, "^\\s*(\\w+)\\s*=\\s*(.*)$")) {
     *     System.out.println("matched.");
     *   }
     *  </pre>
     */
    public static boolean matches(String target, String regexp) {
        Matcher m = matcher(target, regexp);
        return m.find();
    }


    public static boolean matches(String target, Pattern regexp) {
        Matcher m = regexp.matcher(target);
        return m.find();
    }

   /**
     *  split string into list of line
     */
    public static List toListOfLines(String str) {
        List<String> list = new ArrayList<>();
        int len = str.length();
        int head = 0;
        for (int i = 0; i < len; i++) {
            char ch = str.charAt(i);
            if (ch == '\n') {
                int tail = i + 1;
                String line = str.substring(head, tail);
                list.add(line);
                head = tail;
            }
        }
        if (head != len) {
            String line = str.substring(head, len);
            list.add(line);
        }
        return list;
    }

    /**
     *  return true if 'instance' is an instance of 'klass'
     */
    public static boolean isInstanceOf(Object instance, Class klass) {
        if (instance == null || klass == null) {
            return false;
        }
        Class c = instance.getClass();
        if (klass.isInterface()) {
            while (c != null) {
                Class[] interfaces = c.getInterfaces();
                for (Class anInterface : interfaces) {
                    if (anInterface == klass) {
                        return true;
                    }
                }
                c = c.getSuperclass();
            }
        } else {
            while (c != null) {
                if (c == klass) {
                    return true;
                }
                c = c.getSuperclass();
            }
        }
        return false;
    }


    /**
     *  read file content with default encoding of system
     */
    public static String readFile(String filename) throws IOException {
        String charset = System.getProperty("file.encoding");
        return readFile(filename, charset);
    }


    /**
     *  read file content with specified encoding
     */
    private static String readFile(String filename, String encoding) throws IOException {
        String content;
        try (InputStream stream = new FileInputStream(filename)){
            content = readInputStream(stream, encoding);
        }
        return content;
    }

    public static String readInputStream(InputStream stream) throws IOException {
        String encoding = System.getProperty("file.encoding");
        return readInputStream(stream, encoding);
    }

    private static String readInputStream(InputStream stream, String encoding) throws IOException {
        String content;
        try (Reader reader = new InputStreamReader(stream, encoding)){
            StringBuilder sb = new StringBuilder();
            int ch;
            while ((ch = reader.read()) >= 0) {
                sb.append((char)ch);
            }
            content = sb.toString();
        }
        return content;
    }

    public static String untabify(CharSequence str) {
        int tabWidth = 8;
        StringBuilder sb = new StringBuilder();
        int len = str.length();
        int col = -1;
        for (int i = 0; i < len; i++) {
            col = ++col % tabWidth;
            char ch = str.charAt(i);

            switch (ch) {
            case '\t':
                appendTabAsSpaces(tabWidth, sb, col);
                col = -1;
                break;
            case '\n':
                sb.append(ch);
                col = -1;
                break;
            default:
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static void appendTabAsSpaces(int tabWidth, StringBuilder sb, int col) {
        int n = tabWidth - col;
        while (--n >= 0) {
            sb.append(' ');
        }
    }

    public static int compareValues(Object value1, Object value2) {
        int vtype = (valueType(value1) << 8) | valueType(value2);
        switch (vtype) {
        case (VALUE_INTEGER << 8) | VALUE_INTEGER :
            return ((Integer)value1).compareTo((Integer)value2);
        case (VALUE_DOUBLE  << 8) | VALUE_DOUBLE :
            return ((Double)value1).compareTo((Double)value2);
        case (VALUE_STRING  << 8) | VALUE_STRING :
            return ((String)value1).compareTo((String)value2);
        case (VALUE_BOOLEAN << 8) | VALUE_BOOLEAN :
            boolean b1 = (Boolean) value1;
            boolean b2 = (Boolean) value2;
            int ret = b1 ? 1 : -1;
            return (b1 == b2) ? 0 : ret;
        case (VALUE_DATE    << 8) | VALUE_DATE :
            return ((Date)value1).compareTo((Date)value2);
        case (VALUE_DOUBLE  << 8) | VALUE_INTEGER :
        case (VALUE_INTEGER << 8) | VALUE_DOUBLE  :
            double d1 = ((Number)value1).doubleValue();
            double d2 = ((Number)value2).doubleValue();
            return Double.compare(d1, d2);
            default:
                throw new InvalidTypeException("cannot compare '" + value1.getClass().getName() + "' with '" + value2.getClass().getName());
        }
    }

    private static int valueType(Object value) {
        if (value instanceof Integer) {
            return VALUE_INTEGER;
        }

        if (value instanceof Double) {
            return VALUE_DOUBLE;
        }

        if (value instanceof String) {
            return VALUE_STRING;
        }

        if (value instanceof Boolean) {
            return VALUE_BOOLEAN;
        }

        if (value instanceof Date) {
            return VALUE_DATE;
        }

        return VALUE_OBJECT;
    }

    public static String repeatString(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * parse command-line options.
     *
     * ex.
     * <pre>
     *   public static void main(String[] arg) {
     *      String singles = "hv";    // options which takes no argument.
     *      String requireds = "fI";  // options which requires an argument.
     *      String optionals = "i";   // options which can take optional argument.
     *      try {
     *         Object[] ret = parseCommandOptions(args, singles, requireds, optionals);
     *         Map options        = (Map)ret[0];
     *         Map properties     = (Map)ret[1];
     *         String[] filenames = (String[])ret[2];
     *         //...
     *      } catch (CommandOptionException ex) {
     *         char option = ex.getOption();
     *         String error_symbol = ex.getErrorSymbol();
     *         Systen.err.println("*** error: " + ex.getMessage());
     *      }
     *   }
     * </pre>
     *
     * @param args      command-line strings
     * @param singles   options which takes no argument
     * @param requireds options which requires an argument.
     * @param optionals otpions which can take optional argument.
     * @return array of options(Map), properties(Map), and filenames(String[])
     */
    public static Object[] parseCommandOptions(String[] args, String singles, String requireds, String optionals) throws CommandOptionException {
        Map<String, Object> options = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();
        String[] filenames;

        int i;
        for (i = 0; i < args.length; i++) {
            if (args[i].length() == 0 || args[i].charAt(0) != '-') {
                break;
            }
            String opt = args[i];
            int len = opt.length();
            if (len == 1) {   // option '-' means "don't parse arguments!"
                i++;
                break;
            }
            assert len > 1;
            if (opt.charAt(1) == '-') {  // properties (--pname=pvalue)
                String pname;
                Object pvalue;
                int idx = opt.indexOf('=');
                if (idx >= 0) {
                    pname  = opt.substring(2, idx);
                    pvalue = idx + 1 < opt.length() ? opt.substring(idx + 1) : "";
                } else {
                    pname  = opt.substring(2);
                    pvalue = Boolean.TRUE;
                }
                properties.put(pname, pvalue);
            } else {              // command-line options
                for (int j = 1; j < len; j++) {
                    char ch = opt.charAt(j);
                    String chstr = Character.toString(ch);
                    if (singles != null && singles.indexOf(ch) >= 0) {
                        options.put(chstr, Boolean.TRUE);
                    } else if (requireds != null && requireds.indexOf(ch) >= 0) {
                        String arg = null;
                        if (++j < len) {
                            arg = opt.substring(j);
                        } else if (++i < args.length) {
                            arg = args[i];
                        } else {
                            throw new CommandOptionException("-" + ch + ": filename required.", ch, "command.option.noarg");
                        }
                        options.put(chstr, arg);
                        break;
                    } else if (optionals != null && optionals.indexOf(ch) >= 0) {
                        Object arg;
                        if (++j < len) {
                            arg = opt.substring(j);
                        } else {
                            arg = Boolean.TRUE;
                        }
                        options.put(chstr, arg);
                        break;
                    } else {
                        throw new CommandOptionException("-" + ch + "invalid option.", ch, "command.option.invalid");
                    }
                }
            }
        }

        assert i <= args.length;
        int n = args.length - i;
        filenames = new String[n];
        for (int j = 0; i < args.length; i++, j++) {
            filenames[j] = args[i];
        }

        return new Object[] { options, properties, filenames };
    }
}
