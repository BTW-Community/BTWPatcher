package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.Mod;

/**
 * Matches EntityLivingBase class.
 */
public class EntityLivingBaseMod extends com.prupe.mcpatcher.ClassMod {
    public EntityLivingBaseMod(Mod mod) {
        super(mod);
        setParentClass("Entity");

        addClassSignature(new ConstSignature("HealF"));
        addClassSignature(new ConstSignature("Health"));
        addClassSignature(new ConstSignature("ActiveEffects"));
    }
}
