package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.InterfaceMethodRef;
import com.prupe.mcpatcher.JavaRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import javassist.bytecode.AccessFlag;

import java.util.ArrayList;
import java.util.List;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

/**
 * Maps Icon interface.
 */
public class IconMod extends com.prupe.mcpatcher.ClassMod {
    public static JavaRef getWidth;
    public static JavaRef getHeight;
    public static JavaRef getX0;
    public static JavaRef getY0;
    public static JavaRef getMinU;
    public static JavaRef getMaxU;
    public static JavaRef getInterpolatedU;
    public static JavaRef getMinV;
    public static JavaRef getMaxV;
    public static JavaRef getInterpolatedV;
    public static JavaRef getIconName;
    public static JavaRef getSheetWidth;
    public static JavaRef getSheetHeight;

    public static boolean haveClass() {
        return Mod.getMinecraftVersion().compareTo("14w25a") < 0;
    }

    public IconMod(Mod mod) {
        super(mod);

        if (haveClass()) {
            setup17();
        } else {
            setup18();
        }
    }

    private void setup17() {
        List<JavaRef> methods = new ArrayList<JavaRef>();

        if (ResourceLocationMod.haveClass()) {
            getWidth = new InterfaceMethodRef("Icon", "getWidth", "()I");
            getHeight = new InterfaceMethodRef("Icon", "getHeight", "()I");
            getX0 = null;
            getY0 = null;
            getSheetWidth = null;
            getSheetHeight = null;
            methods.add(getWidth);
            methods.add(getHeight);
        } else {
            getWidth = null;
            getHeight = null;
            getX0 = new InterfaceMethodRef("Icon", "getX0", "()I");
            getY0 = new InterfaceMethodRef("Icon", "getY0", "()I");
            getSheetWidth = new InterfaceMethodRef("Icon", "getSheetWidth", "()I");
            getSheetHeight = new InterfaceMethodRef("Icon", "getSheetHeight", "()I");
            methods.add(getX0);
            methods.add(getY0);
        }

        getMinU = new InterfaceMethodRef("Icon", "getMinU", "()F");
        getMaxU = new InterfaceMethodRef("Icon", "getMaxU", "()F");
        getInterpolatedU = new InterfaceMethodRef("Icon", "getInterpolatedU", "(D)F");
        getMinV = new InterfaceMethodRef("Icon", "getMinV", "()F");
        getMaxV = new InterfaceMethodRef("Icon", "getMaxV", "()F");
        getInterpolatedV = new InterfaceMethodRef("Icon", "getInterpolatedV", "(D)F");
        getIconName = new InterfaceMethodRef("Icon", "getIconName", "()Ljava/lang/String;");

        methods.add(getMinU);
        methods.add(getMaxU);
        methods.add(getInterpolatedU);
        methods.add(getMinV);
        methods.add(getMaxV);
        methods.add(getInterpolatedV);
        methods.add(getIconName);

        if (!ResourceLocationMod.haveClass()) {
            methods.add(getSheetWidth);
            methods.add(getSheetHeight);
        }

        InterfaceMethodRef[] methodsArray = new InterfaceMethodRef[methods.size()];
        for (int i = 0; i < methods.size(); i++) {
            methodsArray[i] = (InterfaceMethodRef) methods.get(i);
        }
        addClassSignature(new InterfaceSignature(methodsArray)
                .setInterfaceOnly(true)
        );
    }

    private void setup18() {
        getWidth = new MethodRef("Icon", "getWidth", "()I");
        getHeight = new MethodRef("Icon", "getHeight", "()I");
        getX0 = new MethodRef("Icon", "getX0", "()I");
        getY0 = new MethodRef("Icon", "getY0", "()I");
        getMinU = new MethodRef("Icon", "getMinU", "()F");
        getMaxU = new MethodRef("Icon", "getMaxU", "()F");
        getInterpolatedU = new MethodRef("Icon", "getInterpolatedU", "(D)F");
        getMinV = new MethodRef("Icon", "getMinV", "()F");
        getMaxV = new MethodRef("Icon", "getMaxV", "()F");
        getInterpolatedV = new MethodRef("Icon", "getInterpolatedV", "(D)F");
        getIconName = new MethodRef("Icon", "getIconName", "()Ljava/lang/String;");
        getSheetWidth = null;
        getSheetHeight = null;

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(repeat(build(
                    push(0.009999999776482582),
                    anyILOAD,
                    I2D,
                    DDIV,
                    D2F,
                    anyFSTORE
                ), 2));
            }
        });

        addMemberMapper(new MethodMapper(
                (MethodRef) getX0,
                (MethodRef) getY0,
                (MethodRef) getWidth,
                (MethodRef) getHeight
            )
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
        );

        addMemberMapper(new MethodMapper(
                (MethodRef) getMinU,
                (MethodRef) getMaxU,
                (MethodRef) getMinV,
                (MethodRef) getMaxV
            )
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
        );

        addMemberMapper(new MethodMapper(
                (MethodRef) getInterpolatedU,
                (MethodRef) getInterpolatedV
            )
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
        );

        addMemberMapper(new MethodMapper(
                (MethodRef) getIconName
            )
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
        );
    }
}
