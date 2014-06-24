package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BinaryRegex.begin;
import static com.prupe.mcpatcher.BytecodeMatcher.anyReference;
import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

/**
 * Matches FontRenderer class and maps charWidth, fontTextureName, and spaceWidth fields.
 */
public class FontRendererMod extends com.prupe.mcpatcher.ClassMod {
    public FontRendererMod(Mod mod) {
        super(mod);

        addClassSignature(new BytecodeSignature() {
            {
                matchConstructorOnly(true);
                addXref(1, new FieldRef(getDeobfClass(), "charWidth", "[I"));
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    begin(),
                    ALOAD_0,
                    anyReference(INVOKESPECIAL),
                    ALOAD_0,
                    push(256),
                    NEWARRAY, T_INT,
                    captureReference(PUTFIELD)
                );
            }
        });

        addClassSignature(new OrSignature(
            new ConstSignature("0123456789abcdef"),
            new ConstSignature("0123456789abcdefk"),
            new ConstSignature(ResourceLocationMod.select("/font/glyph_sizes.bin", "font/glyph_sizes.bin"))
        ));
    }
}
