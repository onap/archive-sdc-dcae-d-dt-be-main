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
