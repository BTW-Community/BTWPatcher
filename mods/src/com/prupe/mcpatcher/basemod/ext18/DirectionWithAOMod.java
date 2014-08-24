package com.prupe.mcpatcher.basemod.ext18;

import com.prupe.mcpatcher.ClassMod;
import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.Mod;

public class DirectionWithAOMod extends ClassMod {
    public static final FieldRef aoMultiplier = new FieldRef("DirectionWithAO", "aoMultiplier", "F");

    public DirectionWithAOMod(Mod mod) {
        super(mod);

        addClassSignature(new ConstSignature("DOWN"));
        addClassSignature(new ConstSignature("UP"));
        addClassSignature(new ConstSignature("NORTH"));
        addClassSignature(new ConstSignature("SOUTH"));
        addClassSignature(new ConstSignature("WEST"));
        addClassSignature(new ConstSignature("EAST"));

        addClassSignature(new ConstSignature(0.5f));
        addClassSignature(new ConstSignature(0.6f));
        addClassSignature(new ConstSignature(0.8f));

        addMemberMappers("final !static", aoMultiplier);
    }
}
