/*
 * @(#)ValidationException.java	$Rev: 4 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

public class ValidationException extends BaseException {
    private static final long serialVersionUID = -2991121377463453973L;

    public ValidationException(String message, String path, Object value, Rule rule) {
        super(message, path, value, rule);
    }
}
