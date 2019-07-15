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

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class VesDataTypeDefinition {

	private static final String jsonReferencePrefix = "#/definitions/";
	private String type;
	private String description;
	private String format;
	private String title;
	private Map<String, VesDataTypeDefinition> properties;
	private List<String> required = new ArrayList<>();
	@SerializedName("enum")
	private List<String> enums;
	@SerializedName("default")
	private JsonElement defaultValue;
	private VesDataItemsDefinition items;
	@SerializedName("$ref")
	private String ref;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Map<String, VesDataTypeDefinition> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, VesDataTypeDefinition> properties) {
		this.properties = properties;
	}

	public List<String> getRequired() {
		return required;
	}

	public void setRequired(List<String> required) {
		this.required = required;
	}

	public List<String> getEnums() {
		return enums;
	}

	public void setEnums(List<String> enums) {
		this.enums = enums;
	}

	public JsonElement getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(JsonElement defaultValue) {
		this.defaultValue = defaultValue;
	}

	public VesDataItemsDefinition getItems() {
		return items;
	}

	public void setItems(VesDataItemsDefinition items) {
		this.items = items;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	protected boolean hasReference() {
		return StringUtils.isNotBlank(getRef());
	}

	protected boolean itemsContainReference() {
		return CollectionUtils.isNotEmpty(getItems()) && getItems().stream().anyMatch(VesDataTypeDefinition::containsAnyReferenceItem);
	}

	protected boolean propertiesContainReference() {
		return MapUtils.isNotEmpty(getProperties()) && getProperties().values().stream().anyMatch(VesDataTypeDefinition::containsAnyReferenceItem);
	}

	protected boolean containsAnyReferenceItem() {
		return hasReference() || itemsContainReference() || propertiesContainReference();
	}

	protected String getJsonRefPointer() {
		return getRef().replace(jsonReferencePrefix, "");
	}

	private void addReferenceItem(Set<String> allRefs) {
		if (hasReference()) {
			allRefs.add(getJsonRefPointer());
		}
	}

	private Set<String> extractAllReferenceTokens() {
		Set<String> allRefs = new HashSet<>();
		extractReferenceTokens(allRefs);
		return allRefs;
	}

	private void extractReferenceTokens(Set<String> allRefs) {

		addReferenceItem(allRefs);
		if (itemsContainReference()) {
			getItems().forEach(item -> item.extractReferenceTokens(allRefs));
		}
		if (propertiesContainReference()) {
			getProperties().values().forEach(property -> property.extractReferenceTokens(allRefs));
		}
	}

	protected boolean isResolvable(Map<String, VesDataTypeDefinition> resolvedTypes) {
		return resolvedTypes.keySet().containsAll(extractAllReferenceTokens());
	}

	private void resolveReference(Map<String, VesDataTypeDefinition> resolvedTypes) {
		if (hasReference()) {
			VesDataTypeDefinition other = resolvedTypes.get(getJsonRefPointer());
			setType(other.getType());
			setRef(other.getRef());
			setDefaultValue(other.getDefaultValue());
			setDescription(other.getDescription());
			setEnums(other.getEnums());
			setProperties(other.getProperties());
			setFormat(other.getFormat());
			setRequired(other.getRequired());
			setItems(other.getItems());
			setTitle(other.getTitle());
		}
	}

	private void resolveItemReferences(Map<String, VesDataTypeDefinition> resolvedTypes) {
		if (itemsContainReference()) {
			for (VesDataTypeDefinition item : getItems()) {
				item.resolveAllReferences(resolvedTypes);
			}
		}
	}

	private void resolvePropertyReferences(Map<String, VesDataTypeDefinition> resolvedTypes) {
		if (propertiesContainReference()) {
			for (VesDataTypeDefinition property : getProperties().values()) {
				property.resolveAllReferences(resolvedTypes);
			}
		}
	}

	// the reference resolver is called on each VesDataTypeDefinition after it passes the 'isResolvable' validation, affirming that all its references(direct/properties/items) point to a resolved VesDataTypeDefinition (has no references)
	protected void resolveAllReferences(Map<String, VesDataTypeDefinition> resolvedTypes) {
		resolveReference(resolvedTypes);
		resolveItemReferences(resolvedTypes);
		resolvePropertyReferences(resolvedTypes);
	}

	private String validateType() {
		return null == type? null : VesSimpleTypesEnum.getSimpleTypes().contains(type) ? null : "invalid type declaration: " + type;
	}

	private String validateRequired() {
		String invalid = null == type? null : !type.equals(VesSimpleTypesEnum.OBJECT.getType()) ? null : required.stream().filter(r -> !properties.keySet().contains(r)).findAny().orElse(null);
		return StringUtils.isBlank(invalid) ? invalid : "invalid required entry: " + invalid;
	}

	// returns error message detailing invalid 'type' or 'required' fields (null for success)
	protected String validate() {
		String error = validateType();
		if (StringUtils.isBlank(error))
			error = validateRequired();
		if (StringUtils.isBlank(error) && CollectionUtils.isNotEmpty(items))
			error = validateItems();
		if(StringUtils.isBlank(error) && MapUtils.isNotEmpty(properties))
			error = validateProperties();
		return error;
	}

	private String validateItems(){
		String error = null;
		for (VesDataTypeDefinition def : items) {
			if (StringUtils.isBlank(error))
				error = def.validate();
			else
				break;
		}
		return error;
	}

	private String validateProperties(){
		String error = null;
		for (VesDataTypeDefinition def : properties.values()) {
			if (StringUtils.isBlank(error))
				error = def.validate();
			else
				break;
		}
		return error;
	}


	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (null == obj || getClass() != obj.getClass())
			return false;
		VesDataTypeDefinition other = (VesDataTypeDefinition) obj;
		return Objects.equals(type, other.type) &&
				Objects.equals(description, other.description) &&
				Objects.equals(format, other.format) &&
				Objects.equals(title, other.title) &&
				Objects.equals(required, other.required) &&
				Objects.equals(enums, other.enums) &&
				Objects.equals(defaultValue, other.defaultValue) &&
				Objects.equals(items, other.items) &&
				Objects.equals(properties, other.properties) &&
				Objects.equals(ref, other.ref);
	}

	@Override public int hashCode() {
		int result = type != null ? type.hashCode() : 0;
		result = 31 * result + (description != null ? description.hashCode() : 0);
		result = 31 * result + (format != null ? format.hashCode() : 0);
		result = 31 * result + (title != null ? title.hashCode() : 0);
		result = 31 * result + (properties != null ? properties.hashCode() : 0);
		result = 31 * result + (required != null ? required.hashCode() : 0);
		result = 31 * result + (enums != null ? enums.hashCode() : 0);
		result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
		result = 31 * result + (items != null ? items.hashCode() : 0);
		result = 31 * result + (ref != null ? ref.hashCode() : 0);
		return result;
	}
}
