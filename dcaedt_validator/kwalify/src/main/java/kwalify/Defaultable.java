/*
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

/**
 * interface to have default value
 */
public interface Defaultable {
    Rule getDefault();
    void setDefault(Rule value);
}
