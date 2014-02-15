package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.Mod;

/**
 * Matches EntityLiving class.
 */
public class EntityLivingMod extends com.prupe.mcpatcher.ClassMod {
    public EntityLivingMod(Mod mod) {
        super(mod);
        setParentClass("EntityLivingBase");

        addClassSignature(new ConstSignature("explode"));
        addClassSignature(new ConstSignature("CanPickUpLoot"));
        addClassSignature(new ConstSignature("PersistenceRequired"));
        addClassSignature(new ConstSignature("Equipment"));
    }
}
