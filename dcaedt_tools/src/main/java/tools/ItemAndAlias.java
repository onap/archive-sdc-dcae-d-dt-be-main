package tools;

import org.onap.sdc.dcae.composition.restmodels.sdc.Resource;

public class ItemAndAlias {
    private final Resource item;
    private final String alias;
    public ItemAndAlias(Resource item, String alias) {
        this.item = item;
        this.alias = alias;
    }

    public Resource getItem() {
        return item;
    }

    public String getAlias() {
        return alias;
    }
}
