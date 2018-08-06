package org.onap.sdc.dcae.checker;

import kwalify.Rule;
import kwalify.Validator;

import java.util.Map;

public interface IChecker {
    void check_template_artifacts_definition(
            Map<String, Object> theDefinition,
            Checker.CheckContext theContext);

    /* */
    void check_inputs(Map<String, Map> theInputs,
                      Checker.CheckContext theContext);

    void validationHook(String theTiming,
                        Object theTarget,
                        Rule theRule,
                        Validator.ValidationContext theContext);
}
