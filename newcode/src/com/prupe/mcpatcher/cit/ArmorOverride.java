package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.TexturePackAPI;
import net.minecraft.src.ResourceAddress;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

final class ArmorOverride extends OverrideBase {
    private final Map<ResourceAddress, ResourceAddress> armorMap;

    ArmorOverride(ResourceAddress propertiesName, Properties properties) {
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
            armorMap = new HashMap<ResourceAddress, ResourceAddress>();
            for (Map.Entry<String, ResourceAddress> entry : alternateTextures.entrySet()) {
                String key = entry.getKey();
                ResourceAddress value = entry.getValue();
                if (!key.endsWith(".png")) {
                    key += ".png";
                }
                armorMap.put(TexturePackAPI.parseResourceAddress(propertiesName, key), value);
            }
        }
    }

    @Override
    String getType() {
        return "armor";
    }

    ResourceAddress getReplacementTexture(ResourceAddress origResource) {
        if (armorMap != null) {
            ResourceAddress newResource = armorMap.get(origResource);
            if (newResource != null) {
                return newResource;
            }
        }
        return textureName;
    }
}
