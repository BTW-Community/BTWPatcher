package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.TileLoader;
import net.minecraft.src.Icon;
import net.minecraft.src.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

final class ItemOverride extends OverrideBase {
    private static final int ITEM_ID_COMPASS = 345;
    private static final int ITEM_ID_CLOCK = 347;

    private Icon icon;
    private final Map<Icon, Icon> iconMap;

    ItemOverride(ResourceLocation propertiesName, Properties properties) {
        super(propertiesName, properties);

        if (itemsIDs == null) {
            error("no matching items specified");
        }

        iconMap = alternateTextures == null ? null : new HashMap<Icon, Icon>();
    }

    @Override
    String getType() {
        return "item";
    }

    Icon getReplacementIcon(Icon origIcon) {
        if (iconMap != null) {
            Icon newIcon = iconMap.get(origIcon);
            if (newIcon != null) {
                return newIcon;
            }
        }
        return icon;
    }

    void preload(TileLoader tileLoader) {
        String special = null;
        if (itemsIDs != null) {
            if (itemsIDs.get(ITEM_ID_COMPASS)) {
                special = "compass";
            } else if (itemsIDs.get(ITEM_ID_CLOCK)) {
                special = "clock";
            }
        }
        if (textureName != null) {
            tileLoader.preloadTile(textureName, false, special);
        }
        if (alternateTextures != null) {
            for (Map.Entry<String, ResourceLocation> entry : alternateTextures.entrySet()) {
                tileLoader.preloadTile(entry.getValue(), false, special);
            }
        }
    }

    void registerIcon(TileLoader tileLoader) {
        if (textureName != null) {
            icon = tileLoader.getIcon(textureName);
        }
        if (alternateTextures != null) {
            for (Map.Entry<String, ResourceLocation> entry : alternateTextures.entrySet()) {
                Icon from = tileLoader.getIcon(entry.getKey());
                Icon to = tileLoader.getIcon(entry.getValue());
                if (from != null && to != null) {
                    iconMap.put(from, to);
                }
            }
        }
    }

    @Override
    String preprocessAltTextureKey(String name) {
        if (name.startsWith("textures/items/")) {
            name = name.substring(15);
            if (name.endsWith(".png")) {
                name = name.substring(0, name.length() - 4);
            }
        }
        return name;
    }
}
