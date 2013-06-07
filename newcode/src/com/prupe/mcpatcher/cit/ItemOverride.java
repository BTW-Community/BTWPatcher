package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TileLoader;
import net.minecraft.src.Icon;
import net.minecraft.src.ItemStack;

import java.util.Properties;

final class ItemOverride extends OverrideBase {
    final String matchIcon;
    Icon icon;

    ItemOverride(String propertiesName, Properties properties) {
        super(propertiesName, properties);

        if (itemsIDs == null) {
            error("no matching items specified");
        }
        String value = MCPatcherUtils.getStringProperty(properties, "matchTile", "");
        matchIcon = value.equals("") ? null : value;
    }

    @Override
    String getType() {
        return "item";
    }

    @Override
    boolean match(ItemStack itemStack, Icon origIcon, int[] itemEnchantmentLevels, boolean hasEffect) {
        return super.match(itemStack, origIcon, itemEnchantmentLevels, hasEffect) && matchOrigIcon(origIcon);
    }

    private boolean matchOrigIcon(Icon origIcon) {
        return matchIcon == null || matchIcon.equals(origIcon.getIconName());
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
