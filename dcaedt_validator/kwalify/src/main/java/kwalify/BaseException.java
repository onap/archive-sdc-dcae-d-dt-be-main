/*
 * @(#)BaseException.java $Rev: 3 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

public abstract class BaseException extends KwalifyRuntimeException  {

    private final String yPath;
    private final transient Object value;
    private final transient Rule rule;
    private int lineNum = -1;

    BaseException(String message, String ypath, Object value, Rule rule) {
        super(message);
        this.yPath = ypath;
        this.value = value;
        this.rule  = rule;
    }

    public String getPath() { return "".equals(yPath) ? "/" : yPath; }

    public Object getValue() { return value; }

    public Rule getRule() { return rule; }

    public int getLineNumber() { return lineNum; }

    public void setLineNumber(int lineNum) { this.lineNum = lineNum; }
}
