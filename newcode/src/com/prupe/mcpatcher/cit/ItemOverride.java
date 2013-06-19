package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import com.prupe.mcpatcher.TileLoader;
import net.minecraft.src.Icon;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ResourceAddress;

import java.util.Properties;

final class ItemOverride extends OverrideBase {
    private static final int ITEM_ID_COMPASS = 345;
    private static final int ITEM_ID_CLOCK = 347;

    private final ResourceAddress matchIconName;
    private Icon icon;
    private Icon matchIcon;

    ItemOverride(ResourceAddress propertiesName, Properties properties) {
        super(propertiesName, properties);

        if (itemsIDs == null) {
            error("no matching items specified");
        }
        matchIconName = TexturePackAPI.parseResourceAddress(MCPatcherUtils.getStringProperty(properties, "matchTile", ""));
    }

    @Override
    String getType() {
        return "item";
    }

    Icon getReplacementIcon(Icon origIcon) {
        return icon;
    }

    private boolean matchOrigIcon(Icon origIcon) {
        return matchIcon == null || matchIcon == origIcon;
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
        tileLoader.preloadTile(textureName, false, special);
    }

    void registerIcon(TileLoader tileLoader) {
        icon = tileLoader.getIcon(textureName.getPath());
        if (matchIconName != null) {
            matchIcon = tileLoader.getIcon(matchIconName.getPath());
        }
    }
}
