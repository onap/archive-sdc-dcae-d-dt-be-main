package org.onap.sdc.dcae.enums;


public enum LifecycleOperationType {
	CHECKIN("checkin"), CHECKOUT("checkout"), CERTIFY("certify"), UNDO_CHECKOUT("undoCheckout");

	private String value;

	LifecycleOperationType(String value){
		this.value = value;
	}

	public String getValue(){
		return value;
	}
}
