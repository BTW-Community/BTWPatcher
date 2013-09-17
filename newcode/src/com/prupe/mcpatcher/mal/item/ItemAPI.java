package com.prupe.mcpatcher.mal.item;

import com.prupe.mcpatcher.MAL;
import net.minecraft.src.Item;

import java.util.*;

abstract public class ItemAPI {
    private static final ItemAPI instance = MAL.newInstance(ItemAPI.class, "item");

    public static Item parseItemName(String name) {
        if (name.matches("\\d+")) {
            int id = Integer.parseInt(name);
            return instance.getItemById_Impl(id);
        }
        Set<String> names = new HashSet<String>();
        names.add(name);
        instance.getPossibleItemNames_Impl(name, names);
        for (Item item : getAllItems()) {
            if (instance.matchItem_Impl(item, names)) {
                return item;
            }
        }
        return null;
    }

    public static String getItemName(Item item) {
        return item == null ? "(null)" : item.getItemName().replaceFirst("^item\\.", "");
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

    protected void getPossibleItemNames_Impl(String name, Set<String> aliases) {
    }

    protected boolean matchItem_Impl(Item item, Set<String> names) {
        return names.contains(getItemName(item));
    }

    abstract protected Item getItemById_Impl(int id);

    ItemAPI() {
    }

    final private static class V1 extends ItemAPI {
        @Override
        protected List<Item> getAllItems_Impl() {
            return Arrays.asList(Item.itemsList);
        }

        @Override
        protected void getPossibleItemNames_Impl(String name, Set<String> aliases) {
            if (name.startsWith("minecraft:")) {
                aliases.add(name.substring(10));
            }
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
        protected boolean matchItem_Impl(Item item, Set<String> names) {
            for (String name : names) {
                if (item == Item.itemRegistry.get(name)) {
                    return true;
                }
            }
            return super.matchItem_Impl(item, names);
        }

        @Override
        protected Item getItemById_Impl(int id) {
            return Item.itemRegistry.getById(id);
        }
    }
}
