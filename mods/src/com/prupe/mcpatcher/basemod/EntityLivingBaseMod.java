package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.Mod;

/**
 * Matches EntityLivingBase class.
 */
public class EntityLivingBaseMod extends com.prupe.mcpatcher.ClassMod {
    public EntityLivingBaseMod(Mod mod) {
        super(mod);
        setParentClass("Entity");

        addClassSignature(new ConstSignature("Health"));
        if (ResourceLocationMod.haveClass()) {
            addClassSignature(new ConstSignature("HealF"));
            addClassSignature(new ConstSignature("ActiveEffects"));
        } else {
            addClassSignature(new ConstSignature("/mob/char.png"));
        }
    }
}
