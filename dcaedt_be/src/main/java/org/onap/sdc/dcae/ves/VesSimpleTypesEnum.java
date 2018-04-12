package org.onap.sdc.dcae.ves;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum VesSimpleTypesEnum {

	ARRAY("array"), BOOLEAN("boolean"), INTEGER("integer"), NULL("null"), NUMBER("number"), OBJECT("object"), STRING("string");

	private String type;

	public String getType() {
		return type;
	}

	private VesSimpleTypesEnum(String type) {
		this.type = type;
	}

	public static Set<String> getSimpleTypes() {
		return Arrays.stream(VesSimpleTypesEnum.values()).map(t -> t.getType()).collect(Collectors.toSet());
	}


}
