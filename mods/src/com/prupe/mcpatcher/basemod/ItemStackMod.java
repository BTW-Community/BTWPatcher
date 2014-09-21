package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.ClassMod;
import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BinaryRegex.begin;
import static com.prupe.mcpatcher.BinaryRegex.end;
import static com.prupe.mcpatcher.BinaryRegex.subset;
import static com.prupe.mcpatcher.BytecodeMatcher.anyReference;
import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

public class ItemStackMod extends ClassMod {
    public static final FieldRef stackSize = new FieldRef("ItemStack", "stackSize", "I");
    public static final FieldRef itemDamage = new FieldRef("ItemStack", "itemDamage", "I");
    public static final MethodRef getTagCompound = new MethodRef("ItemStack", "getTagCompound", "()LNBTTagCompound;");
    public static final MethodRef getItemDamage = new MethodRef("ItemStack", "getItemDamage", "()I");
    public static final MethodRef getItem = new MethodRef("ItemStack", "getItem", "()LItem;");

    public ItemStackMod(Mod mod) {
        super(mod);

        addClassSignature(new ConstSignature("id"));
        addClassSignature(new ConstSignature("Count"));
        addClassSignature(new ConstSignature("Damage"));

        addClassSignature(new BytecodeSignature() {
            {
                matchConstructorOnly(true);
                addXref(1, stackSize);
                addXref(2, itemDamage);
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    ALOAD_0,
                    subset(true,
                        ILOAD_1 /* pre-13w36a */,
                        ALOAD_1 /* 13w36a+ */
                    ),
                    anyReference(PUTFIELD),
                    ALOAD_0,
                    ILOAD_2,
                    captureReference(PUTFIELD),
                    ALOAD_0,
                    ILOAD_3,
                    captureReference(PUTFIELD)
                );
            }
        });

        addClassSignature(new BytecodeSignature() {
            {
                setMethod(getItemDamage);
                addXref(1, itemDamage);
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    begin(),
                    ALOAD_0,
                    captureReference(GETFIELD),
                    IRETURN,
                    end()
                );
            }
        });

        addMemberMapper(new MethodMapper(getTagCompound));
        addMemberMapper(new MethodMapper(getItem));
    }
}
