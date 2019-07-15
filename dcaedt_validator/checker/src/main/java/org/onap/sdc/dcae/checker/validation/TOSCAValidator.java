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

package org.onap.sdc.dcae.checker.validation;

import kwalify.Rule;
import kwalify.Types;
import kwalify.Validator;
import org.onap.sdc.common.onaplog.enums.LogLevel;
import org.onap.sdc.common.onaplog.OnapLoggerDebug;
import org.onap.sdc.common.onaplog.OnapLoggerError;
import org.onap.sdc.dcae.checker.IChecker;
import org.onap.sdc.dcae.checker.Target;

import java.util.*;

public class TOSCAValidator extends Validator {
    private static OnapLoggerError errLogger = OnapLoggerError.getInstance();
    private static OnapLoggerDebug debugLogger = OnapLoggerDebug.getInstance();
    private final IChecker checker;
    //what were validating
    private Target target;

    /* Some of the TOSCA entries accept a 'short form/notation' instead of the canonical map representation.
     * kwalify cannot easily express these alternatives and as such we handle them here. In the pre-validation phase we detect the presence of a short notation
and compute the canonical form and validate it. In the post-validation phase we
substitute the canonical form for the short form so that checking does not have to deal with it.
     */

    public Map<String, Object> getCanonicals() {
        return canonicals;
    }

    private Map<String, Object> canonicals = new TreeMap<>();

    public TOSCAValidator(Target theTarget, Object theSchema, IChecker checker) {
        super(theSchema);
        this.checker = checker;
        this.target = theTarget;
    }

    public Target getTarget() {
        return this.target;
    }

    /* hook method called by Validator#validate()
     */
    @Override
    protected boolean preValidationHook(Object value, Rule rule, ValidationContext context) {

        checker.validationHook("pre", value, rule, context);
        //short form handling
        String hint = rule.getShort();
        if (value != null &&
                hint != null) {

            debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Attempting canonical at {}, rule {}", context.getPath(), rule.getName());

            Object canonical = null;
            //if the canonical form requires a collection
            if (Types.isCollectionType(rule.getType())) {
                //and the actual value isn't one
                if (!(value instanceof Map || value instanceof List)) {
                    //used to use singleton map/list here (was good for catching errors)
                    //but there is the possibility if short forms within short forms so
                    //the created canonicals need to accomodate other values.
                    if (Types.isMapType(rule.getType())) {
                        canonical = new HashMap();
                        ((Map) canonical).put(hint, value);
                    } else {
                        //the hint is irrelevant here but we should impose a value when the target is a list
                        canonical = new LinkedList();
                        ((List) canonical).add(value);
                    }
                } else {
                    //we can accomodate:
                    // map to list of map transformation
                    if (!Types.isMapType(rule.getType()) /* a seq */ &&
                            value instanceof Map) {
                        canonical = new LinkedList();
                        ((List) canonical).add(value);
                    } else {
                        debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Grammar for rule {} (at {}) would require unsupported short form transformation: {} to {}", rule.getName(), context.getPath(), value.getClass(), rule.getType());
                        return false;
                    }
                }

                int errc = context.errorCount();
                validateRule(canonical, rule, context);
                if (errc != context.errorCount()) {
                    debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Short notation for {} through {} at {} failed validation", rule.getName(), hint, context.getPath());
                } else {
                    debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Short notation for {} through {} at {} passed validation. Canonical form is {}", rule.getName(), hint, context.getPath(), canonical);
                    //replace the short notation with the canonicall one so we don't
                    //have to deal it again during checking
                    this.canonicals.put(context.getPath(), canonical);
                    return true;
                }
            } else {
                debugLogger.log(LogLevel.DEBUG, this.getClass().getName(), "Grammar for rule {} (at {}) would require unsupported short form transformation: {} to {}", rule.getName(), context.getPath(), value.getClass(), rule.getType());
            }
        }

        //perform default validation process
        return false;
    }

    /*
     * Only gets invoked once the value was succesfully verified against the syntax indicated by the given rule.
     */
    @Override
    protected void postValidationHook(Object value,
                                      Rule rule,
                                      ValidationContext context) {
        checker.validationHook("post", value, rule, context);
    }

}
