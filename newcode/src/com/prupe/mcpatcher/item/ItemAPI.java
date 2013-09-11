package com.prupe.mcpatcher.item;

import com.prupe.mcpatcher.MAL;
import net.minecraft.src.Item;

abstract public class ItemAPI {
    private static final ItemAPI instance = MAL.newInstance(ItemAPI.class, "item");

    public static Item getItemById(int id) {
        return instance.getItemById_Impl(id);
    }

    public static int getItemId(Item item) {
        return instance.getItemId_Impl(item);
    }

    public static int getNumItems() {
        return instance.getNumItems_Impl();
    }

    abstract protected Item getItemById_Impl(int id);

    abstract protected int getItemId_Impl(Item item);

    abstract protected int getNumItems_Impl();

    ItemAPI() {
    }

    final private static class V1 extends ItemAPI {
        @Override
        protected Item getItemById_Impl(int id) {
            return Item.itemsList[id];
        }

        @Override
        protected int getItemId_Impl(Item item) {
            return item.itemID;
        }

        @Override
        protected int getNumItems_Impl() {
            return Item.itemsList.length;
        }
    }

    final private static class V2 extends ItemAPI {
        @Override
        protected Item getItemById_Impl(int id) {
            return Item.itemRegistry.getById(id);
        }

        @Override
        protected int getItemId_Impl(Item item) {
            return Item.itemRegistry.getId(item);
        }

        @Override
        protected int getNumItems_Impl() {
            return 32000;
        }
    }
}
