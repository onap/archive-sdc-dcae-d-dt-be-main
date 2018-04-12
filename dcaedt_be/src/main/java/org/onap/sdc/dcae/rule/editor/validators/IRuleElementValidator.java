package org.onap.sdc.dcae.rule.editor.validators;

import org.onap.sdc.dcae.errormng.ResponseFormat;

import java.util.List;

public interface IRuleElementValidator <T> {
	boolean validate(T element, List<ResponseFormat> errors);
}
