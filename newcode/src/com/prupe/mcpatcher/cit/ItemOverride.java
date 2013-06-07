package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.TileLoader;
import net.minecraft.src.Icon;

import java.util.Properties;

class ItemOverride extends OverrideBase {
    Icon icon;

    ItemOverride(String propertiesName, Properties properties) {
        super(propertiesName, properties);
    }

    @Override
    String getType() {
        return "item";
    }

    void preload(TileLoader tileLoader) {
        String special = null;
        if (itemsIDs != null) {
            if (itemsIDs.get(CITUtils.ITEM_ID_COMPASS)) {
                special = "compass";
            } else if (itemsIDs.get(CITUtils.ITEM_ID_CLOCK)) {
                special = "clock";
            }
        }
        tileLoader.preloadTile(textureName, false, special);
    }

    void registerIcon(TileLoader tileLoader) {
        icon = tileLoader.getIcon(textureName);
    }
}
