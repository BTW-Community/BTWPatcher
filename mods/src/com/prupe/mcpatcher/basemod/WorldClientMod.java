package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.Mod;

/**
 * Matches WorldClient class.
 */
public class WorldClientMod extends com.prupe.mcpatcher.ClassMod {
    public WorldClientMod(Mod mod) {
        super(mod);
        setParentClass("World");

        addClassSignature(new ConstSignature("MpServer"));
    }
}
