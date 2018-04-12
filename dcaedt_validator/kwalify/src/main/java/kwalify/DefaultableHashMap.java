/*
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

import java.io.Serializable;
import java.util.HashMap;

/**
 * hash map which can have default value
 */
public class DefaultableHashMap extends HashMap implements Defaultable {

    private static final long serialVersionUID = -5224819562023897380L;

    private Object defaultValue = null;

    public DefaultableHashMap() {
        super();
    }

    public Object getDefault() { return defaultValue; }

    public void setDefault(Object value) { defaultValue = value; }

    @Override
    public Object get(Object key) {
        return containsKey(key) ? super.get(key) : defaultValue;
    }
}
