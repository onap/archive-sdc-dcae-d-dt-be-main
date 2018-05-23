/*
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

import java.util.ResourceBundle;

/**
 * set of utility methods around messages.
 *
 */
public class Messages {

    private static final String KWALIFY_MESSAGES = "kwalify.messages";
    private static ResourceBundle __messages = ResourceBundle.getBundle(KWALIFY_MESSAGES);

    // So that no one instantiate Messages and make sonar happy
    private Messages(){}

    public static String message(String key) {
        return __messages.getString(key);
    }

    public static String buildMessage(String key, Object[] args) {
        return buildMessage(key, null, args);
    }

    public static String buildMessage(String key, Object value, Object[] args) {
        String msg = message(key);
        if (args != null) {
            for (Object arg : args) {  // don't use MessageFormat
                msg = msg.replaceFirst("%[sd]", escape(arg));
            }
        }
        if (value != null && !Types.isCollection(value)) {
            msg = "'" + escape(value) + "': " + msg;
        }
        return msg;
    }

    private static String escape(Object obj) {
        return obj.toString().replaceAll("\\\\", "\\\\\\\\").replaceAll("\\n", "\\\\n");
    }
}
