package com.prupe.mcpatcher.item;

import com.prupe.mcpatcher.MAL;

abstract public class ItemAPI {
    private static final ItemAPI instance = MAL.newInstance(ItemAPI.class, "item");

    ItemAPI() {
    }

    final private static class V1 extends ItemAPI {
    }

    final private static class V2 extends ItemAPI {
    }
}
