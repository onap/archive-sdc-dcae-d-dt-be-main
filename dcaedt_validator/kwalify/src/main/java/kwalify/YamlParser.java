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

import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Iterator;

/**
 *  yaml parser which can keep line number of path.
 */
public class YamlParser extends PlainYamlParser {
    private Map linenumsTable = new IdentityHashMap(); // object => sequence or mapping
    private int firstLinenum = -1;
    private Object document = null;

    YamlParser(String yamlStr) {
        super(yamlStr);
    }

    public Object parse() throws SyntaxException {
        document = super.parse();
        return document;
    }

    protected String getLine() {
        String line = super.getLine();
        if (firstLinenum < 0) {
            firstLinenum = currentLineNumber();
        }
        return line;
    }


    private int getPathLineNumber(String ypath) throws InvalidPathException {
        if (document == null) {
            return -1;
        }
        if (ypath.length() == 0 || "/".equals(ypath)) {
            return 1;
        }
        String[] elems = ypath.split("/");
        String lastElem = elems.length > 0 ? elems[elems.length - 1] : null;
        int i = ypath.charAt(0) == '/' ? 1 : 0;
        int len = elems.length - 1;
        Object documentCollection = this.document;   // collection
        for ( ; i < len ; i++) {
            if (documentCollection == null) {
                throw new InvalidPathException(ypath);
            } else if (documentCollection instanceof Map) {
                documentCollection = ((Map)documentCollection).get(elems[i]);
            } else if (documentCollection instanceof List) {
                int index = Integer.parseInt(elems[i]);
                if (index < 0 || ((List)documentCollection).size() < index) {
                    throw new InvalidPathException(ypath);
                }
                documentCollection = ((List)documentCollection).get(index);
            } else {
                throw new InvalidPathException(ypath);
            }
        }

        if (documentCollection == null) {
            throw new InvalidPathException(ypath);
        }
        Object linenums = linenumsTable.get(documentCollection); // Map or List
        int linenum;
        if (documentCollection instanceof Map) {
            assert linenums instanceof Map;
            Object d = ((Map)linenums).get(lastElem);
            linenum = (Integer) d;
        } else if (documentCollection instanceof List) {
            assert linenums instanceof List;
            int index = Integer.parseInt(lastElem);
            if (index < 0 || ((List)linenums).size() <= index) {
                throw new InvalidPathException(ypath);
            }
            Object d = ((List)linenums).get(index);
            linenum = (Integer) d;
        } else {
            throw new InvalidPathException(ypath);
        }
        return linenum;
    }

    public void setErrorsLineNumber(List errors) throws InvalidPathException {
        for (Iterator it = errors.iterator(); it.hasNext(); ) {
            ValidationException ex = (ValidationException)it.next();
            ex.setLineNumber(getPathLineNumber(ex.getPath()));
        }
    }

    protected Map createMapping() {
        Map map = new DefaultableHashMap();
        linenumsTable.put(map, new HashMap());
        return map;
    }
}
