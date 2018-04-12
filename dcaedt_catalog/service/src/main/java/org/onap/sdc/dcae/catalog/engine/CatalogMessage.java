package org.onap.sdc.dcae.catalog.engine;

import java.util.UUID;

public class CatalogMessage {

	private UUID 		id;
	private long 		timestamp = 0;


	public void setId(UUID theId) {
		this.id = theId;
	}

	public UUID getId() {
		return this.id;
	}

	public void setTimestamp(long theTimestamp) {
		this.timestamp = theTimestamp;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

}
