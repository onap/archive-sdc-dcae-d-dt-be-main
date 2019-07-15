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

package org.onap.sdc.dcae.errormng;

public enum ActionStatus {

	OK,
	CREATED,
	NO_CONTENT,
	NOT_ALLOWED,
	GENERAL_ERROR,
	INVALID_CONTENT,
	NOT_FOUND,
	CONFIGURATION_ERROR,
	VES_SCHEMA_NOT_FOUND,
	VES_SCHEMA_INVALID,
	FLOW_TYPES_CONFIGURATION_ERROR,
	CLONE_FAILED,
	EMPTY_SERVICE_LIST,
	MONITORING_TEMPLATE_ATTACHMENT_ERROR,
	MISSING_TOSCA_FILE,
	VALIDATE_TOSCA_ERROR,
	SUBMIT_BLUEPRINT_ERROR,
	GENERATE_BLUEPRINT_ERROR,
	INVALID_RULE_FORMAT,
	SAVE_RULE_FAILED,
	RESOURCE_NOT_VFCMT_ERROR,
	VFI_FETCH_ERROR,
	USER_CONFLICT,
	MISSING_RULE_DESCRIPTION,
	MISSING_ACTION,
	MISSING_ACTION_FIELD,
	MISSING_CONCAT_VALUE,
	INVALID_GROUP_CONDITION,
	MISSING_CONDITION_ITEM,
	MISSING_OPERAND,
	INVALID_OPERATOR,
	MISSING_ENTRY,
	MISSING_DEFAULT_VALUE,
	DUPLICATE_KEY,
	ACTION_DEPENDENCY,
	RULE_DEPENDENCY,
	NODE_NOT_FOUND,
	DELETE_RULE_FAILED,
	FILTER_NOT_FOUND,
	RULE_OPERATION_FAILED_MISSING_PARAMS,
	TRANSLATE_FAILED,
	CATALOG_NOT_AVAILABLE,
	AUTH_ERROR,
	DELETE_BLUEPRINT_FAILED,
	AS_IS
}
