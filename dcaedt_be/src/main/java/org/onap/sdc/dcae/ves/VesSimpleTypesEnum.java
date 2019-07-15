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
