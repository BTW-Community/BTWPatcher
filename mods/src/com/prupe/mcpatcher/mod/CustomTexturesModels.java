package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.Mod;
import com.prupe.mcpatcher.basemod.BlockMod;
import com.prupe.mcpatcher.basemod.ItemMod;
import com.prupe.mcpatcher.mal.TexturePackAPIMod;

public class CustomTexturesModels extends Mod {
    public CustomTexturesModels() {
        name = MCPatcherUtils.CUSTOM_TEXTURES_MODELS;
        author = "MCPatcher";
        description = "Allows custom block and item rendering.";
        version = "1.0";

        addDependency(MCPatcherUtils.TEXTURE_PACK_API_MOD);
        addDependency(MCPatcherUtils.TILESHEET_API_MOD);
        addDependency(MCPatcherUtils.BLOCK_API_MOD);
        addDependency(MCPatcherUtils.ITEM_API_MOD);
        addDependency(MCPatcherUtils.NBT_API_MOD);
        addDependency(MCPatcherUtils.BIOME_API_MOD);

        addClassMod(new BlockMod(this));

        addClassMod(new ItemMod(this));

        addClassFiles("com.prupe.mcpatcher.colormap.*");
        addClassFiles("com.prupe.mcpatcher.ctm.*");
        addClassFiles("com.prupe.mcpatcher.cit.*");

        TexturePackAPIMod.earlyInitialize(2, MCPatcherUtils.CTM_UTILS18_CLASS, "reset");
    }

    @Override
    public String[] getLoggingCategories() {
        return new String[]{
            MCPatcherUtils.CONNECTED_TEXTURES,
            MCPatcherUtils.CUSTOM_ITEM_TEXTURES
        };
    }
}
