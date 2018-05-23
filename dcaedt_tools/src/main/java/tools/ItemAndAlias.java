package tools;

import json.response.ItemsResponse.Item;

public class ItemAndAlias {
    private final Item item;
    private final String alias;
    public ItemAndAlias(Item item, String alias) {
        this.item = item;
        this.alias = alias;
    }

    public Item getItem() {
        return item;
    }

    public String getAlias() {
        return alias;
    }
}
