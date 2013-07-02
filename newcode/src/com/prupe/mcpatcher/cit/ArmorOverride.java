package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.TexturePackAPI;
import net.minecraft.src.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

final class ArmorOverride extends OverrideBase {
    private final Map<ResourceLocation, ResourceLocation> armorMap;

    ArmorOverride(ResourceLocation propertiesName, Properties properties) {
        super(propertiesName, properties);

        if (itemsIDs == null) {
            error("no matching items specified");
        }
        if (textureName == null && alternateTextures == null) {
            error("no replacement textures specified");
        }

        if (alternateTextures == null) {
            armorMap = null;
        } else {
            armorMap = new HashMap<ResourceLocation, ResourceLocation>();
            for (Map.Entry<String, ResourceLocation> entry : alternateTextures.entrySet()) {
                String key = entry.getKey();
                ResourceLocation value = entry.getValue();
                if (!key.endsWith(".png")) {
                    key += ".png";
                }
                armorMap.put(TexturePackAPI.parseResourceLocation(propertiesName, key), value);
            }
        }
    }

    @Override
    String getType() {
        return "armor";
    }

    ResourceLocation getReplacementTexture(ResourceLocation origResource) {
        if (armorMap != null) {
            ResourceLocation newResource = armorMap.get(origResource);
            if (newResource != null) {
                return newResource;
            }
        }
        return textureName;
    }
}
