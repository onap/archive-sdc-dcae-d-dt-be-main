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

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EventListenerDefinition extends VesDataTypeDefinition {

	public static final String EVENT_ROOT = "event";
	private String $schema;
	private Map<String, VesDataTypeDefinition> definitions;

	public String get$schema() {
		return $schema;
	}

	public void set$schema(String $schema) {
		this.$schema = $schema;
	}

	public Map<String, VesDataTypeDefinition> getDefinitions() {
		return definitions;
	}

	public void setDefinitions(Map<String, VesDataTypeDefinition> definitions) {
		this.definitions = definitions;
	}

	// returns error message detailing unresolvable types - or null (success)
	public String resolveRefTypes() {

		Predicate<Map.Entry<String, VesDataTypeDefinition>> isFullyResolved = dt -> !dt.getValue().containsAnyReferenceItem();
		Map<String, VesDataTypeDefinition> resolved = definitions.entrySet().stream()
				.filter(isFullyResolved)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		int initialUnresolvedItems = -1;
		int remainingUnresolvedItems = 0;

		while (resolved.size() != definitions.size() && initialUnresolvedItems != remainingUnresolvedItems) {
			initialUnresolvedItems = definitions.size() - resolved.size();
			definitions.entrySet().forEach(definition -> {
				if (!resolved.containsKey(definition.getKey()) && definition.getValue().isResolvable(resolved)) {
					definition.getValue().resolveAllReferences(resolved);
					resolved.put(definition.getKey(), definition.getValue());
				}
			});
			remainingUnresolvedItems = definitions.size() - resolved.size();
		}

		if (resolved.size() != definitions.size()) {
			definitions.keySet().removeAll(resolved.keySet());
			return constructErrorMessage(definitions.keySet());
		}
		return resolveRootRefTypes();

	}

	private String constructErrorMessage(Set<String> unresolvable) {
		return "the following definitions containing unresolvable references: " + new Gson().toJson(unresolvable);
	}

	private String resolveRootRefTypes() {
		Set<String> unresolvable = new HashSet<>();
		getProperties().forEach((k, v) -> {
			if (isResolvable(definitions))
				resolveAllReferences(definitions);
			else
				unresolvable.add(k);
		});
		return unresolvable.isEmpty() ? null : constructErrorMessage(unresolvable);

	}

	@Override
	public String validate() {
		String error = getProperties().containsKey(EVENT_ROOT) ? null : "schema not containing property: event";
		if (StringUtils.isBlank(error))
			error = super.validate();
		if (StringUtils.isBlank(error))
			error = validateDefinitions();
		return error;
	}

	private String validateDefinitions() {
		String error = null;
		for (VesDataTypeDefinition def : definitions.values()) {
			if (StringUtils.isBlank(error))
				error = def.validate();
			else
				break;
		}
		return error;
	}

}
