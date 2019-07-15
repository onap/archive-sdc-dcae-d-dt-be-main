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

package org.onap.sdc.dcae.checker.common;

import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.dcae.checker.*;

import java.util.Map;

public class DataCommon extends BaseCommon {

    private static DataCommon instance;

    public synchronized static DataCommon getInstance() {
        if (instance == null)
        {
            instance = new DataCommon();
        }
        return instance;
    }

    private DataCommon() {}

    /* the type can be:
     *   a known type: predefined or user-defined
     *   a collection (list or map) and then check that the entry_schema points to one of the first two cases (is that it?)
     */
    public boolean checkDataType(Map theSpec, Checker.CheckContext theContext, Catalog catalog) {
        TypeCommon typeCommon = TypeCommon.getInstance();
        if (!typeCommon.checkType(Construct.Data, theSpec, theContext, catalog)) {
            return false;
        }

        String type = (String) theSpec.get("type");
        if (/*isCollectionType(type)*/
                "list".equals(type) || "map".equals(type)) {
            Map entrySchema = (Map) theSpec.get("entry_schema");
            if (entrySchema == null) {
                //maybe issue a warning ?? or is 'string' the default??
                return true;
            }

            if (!catalog.hasType(Construct.Data, (String) entrySchema.get("type"))) {
                theContext.addError("Unknown entry_schema type: " + entrySchema, null);
                return false;
            }
        }
        return true;
    }

    /*
     * For inputs/properties/attributes/(parameters). It is the caller's
     * responsability to provide the value (from a 'default', inlined, ..)
     *
     * @param theDef the definition of the given construct/facet as it appears in
     * 			its enclosing type definition.
     * @param
     */
    public boolean checkDataValuation(Object theExpr,
                                       Map<String, ?> theDef,
                                       Checker.CheckContext theContext) {
        //first check if the expression is a function, if not handle it as a value assignment
        Data.Function f = Data.function(theExpr);
        if (f != null) {
            return f.evaluator()
                    .eval(theExpr, theDef, theContext);
        } else {
            Data.Type type = Data.typeByName((String) theDef.get("type"));
            if (type != null) {
                Data.Evaluator evaluator;

                evaluator = type.evaluator();
                if (evaluator == null) {
                    debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "No value evaluator available for type {}", type);
                } else {
                    if ((theExpr != null) && (!evaluator.eval(theExpr, theDef, theContext))) {
                        return false;
                    }
                }


                evaluator = type.constraintsEvaluator();
                if (evaluator == null) {
                    debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "No constraints evaluator available for type {}", type);
                } else {
                    if (theExpr != null) {
                        if (!evaluator.eval(theExpr, theDef, theContext)) {
                            return false;
                        }
                    } else {
                        //should have a null value validatorT
                    }
                }

                return true;
            } else {
                theContext.addError("Expression " + theExpr + " of " + theDef + " could not be evaluated", null);
                return false;
            }
        }
    }


}
