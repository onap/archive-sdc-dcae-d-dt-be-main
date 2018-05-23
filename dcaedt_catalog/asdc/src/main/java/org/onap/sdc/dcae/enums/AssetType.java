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
