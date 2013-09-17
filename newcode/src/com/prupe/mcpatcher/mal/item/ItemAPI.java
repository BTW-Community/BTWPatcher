package com.prupe.mcpatcher.mal.item;

import com.prupe.mcpatcher.MAL;
import net.minecraft.src.Item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract public class ItemAPI {
    private static final ItemAPI instance = MAL.newInstance(ItemAPI.class, "item");

    public static Item parseItemName(String name) {
        if (name.matches("\\d+")) {
            int id = Integer.parseInt(name);
            return instance.getItemById_Impl(id);
        }
        name = instance.getCanonicalName_Impl(name);
        for (Item item : getAllItems()) {
            if (name.equals(getItemName(item))) {
                return item;
            }
        }
        return null;
    }

    public static String getItemName(Item item) {
        return item.getItemName();
    }

    public static List<Item> getAllItems() {
        List<Item> items = new ArrayList<Item>();
        for (Item item : instance.getAllItems_Impl()) {
            if (item != null && !items.contains(item)) {
                items.add(item);
            }
        }
        return items;
    }

    abstract protected List<Item> getAllItems_Impl();

    abstract protected String getCanonicalName_Impl(String name);

    abstract protected Item getItemById_Impl(int id);

    ItemAPI() {
    }

    final private static class V1 extends ItemAPI {
        @Override
        protected List<Item> getAllItems_Impl() {
            return Arrays.asList(Item.itemsList);
        }

        @Override
        protected String getCanonicalName_Impl(String name) {
            if (name.startsWith("minecraft:")) {
                name = name.substring(10);
            }
            return name;
        }

        @Override
        protected Item getItemById_Impl(int id) {
            return id >= 0 && id < Item.itemsList.length ? Item.itemsList[id] : null;
        }
    }

    final private static class V2 extends ItemAPI {
        @Override
        protected List<Item> getAllItems_Impl() {
            return Item.itemRegistry.getAll();
        }

        @Override
        protected String getCanonicalName_Impl(String name) {
            if (!name.contains(":")) {
                name = "minecraft:" + name;
            }
            return name;
        }

        @Override
        protected Item getItemById_Impl(int id) {
            return Item.itemRegistry.getById(id);
        }
    }
}
