/*
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

import java.util.HashMap;

/**
 * hash map which can have default value
 */
public class DefaultableHashMap extends HashMap implements Defaultable {

    private static final long serialVersionUID = -5224819562023897380L;

    private Rule defaultValue;

    public DefaultableHashMap() {
        super();
    }

    public Rule getDefault() { return defaultValue; }

    public void setDefault(Rule value) { defaultValue = value; }

    @Override
    public Object get(Object key) {
        return containsKey(key) ? (Rule)super.get(key) : defaultValue;
    }
}
