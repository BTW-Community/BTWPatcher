package com.prupe.mcpatcher.converter;

import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.UserInterface;

import java.io.File;

public class TexturePackConverter16 extends TexturePackConverter {
    public TexturePackConverter16(File input) {
        super(input);
        File dir = MCPatcherUtils.getMinecraftPath("resourcepacks");
        dir.mkdirs();
        output = new File(dir, input.getName());
    }

    @Override
    public void convertImpl(UserInterface ui) throws Exception {
        throw new UnsupportedOperationException("not implemented");
    }
}
