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

package org.onap.sdc.dcae.enums;

public enum AssetType {

	RESOURCE(SdcContextPath.RESOURCES),
	SERVICE(SdcContextPath.SERVICES),
	VFCMT(SdcContextPath.RESOURCES),
	VF(SdcContextPath.RESOURCES);

	private String sdcContextPath;

	AssetType(SdcContextPath sdcContextPath) {
		this.sdcContextPath = sdcContextPath.name().toLowerCase();
	}

	public String getSdcContextPath() {
		return sdcContextPath;
	}

	// passing an invalid type will result in an IllegalArgumentException, which is fine as the 'type' value is an SDC enum value passed from SDC pluggable UI
	public static AssetType getAssetTypeByName(String type) {
		return AssetType.valueOf(type.toUpperCase());
	}

	public static String getSdcContextPath(String type) {
		return getAssetTypeByName(type).getSdcContextPath();
	}

	private enum SdcContextPath {
		RESOURCES, SERVICES
	}
}
