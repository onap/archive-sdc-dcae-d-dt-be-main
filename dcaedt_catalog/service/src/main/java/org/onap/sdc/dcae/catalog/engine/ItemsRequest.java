package org.onap.sdc.dcae.catalog.engine;

public class ItemsRequest extends CatalogRequest {

    public static final ItemsRequest EMPTY_REQUEST = new ItemsRequest("Superportfolio");

    private String startingLabel;

    private ItemsRequest(String theLabel) {
        this.startingLabel = theLabel;
    }

    public String getStartingLabel() {
        return this.startingLabel == null ? "Superportfolio" : this.startingLabel;
    }
}
