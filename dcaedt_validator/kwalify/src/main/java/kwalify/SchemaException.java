/*
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

/**
 * exception class thrown by Rule constructor
 */
public class SchemaException extends BaseException {
    private static final long serialVersionUID = 4750598728284538818L;

    public SchemaException(String message, String ypath, Object value, Rule rule) {
        super(message, ypath, value, rule);
    }

}
