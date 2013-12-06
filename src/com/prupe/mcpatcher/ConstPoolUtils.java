package com.prupe.mcpatcher;

import javassist.bytecode.ConstPool;
import javassist.bytecode.Mnemonic;

import java.util.ArrayList;

import static javassist.bytecode.Opcode.*;

class ConstPoolUtils {
    static final byte[] CONSTANT_OPCODES = new byte[]{(byte) LDC, (byte) LDC_W, (byte) LDC2_W};
    static final byte[] CLASSREF_OPCODES = new byte[]{(byte) NEW, (byte) ANEWARRAY, (byte) CHECKCAST, (byte) INSTANCEOF, (byte) MULTIANEWARRAY};
    static final byte[] FIELDREF_OPCODES = new byte[]{(byte) GETFIELD, (byte) GETSTATIC, (byte) PUTFIELD, (byte) PUTSTATIC};
    static final byte[] METHODREF_OPCODES = new byte[]{(byte) INVOKEVIRTUAL, (byte) INVOKESTATIC, (byte) INVOKESPECIAL};
    static final byte[] INTERFACEMETHODREF_OPCODES = new byte[]{(byte) INVOKEINTERFACE};

    static final String DESCRIPTOR_TYPES = "VZBCSIJFD";
    static final String DESCRIPTOR_CHARS = DESCRIPTOR_TYPES + "()<>[";

    private static final byte[] NOT_FOUND = null;

    private static final int MAX_LDC_INDEX = 255;

    private static final byte[] ARRAY_ACONST_NULL = new byte[]{ACONST_NULL};
    private static final byte[] ARRAY_ICONST_M1 = new byte[]{ICONST_M1};
    private static final byte[] ARRAY_ICONST_0 = new byte[]{ICONST_0};
    private static final byte[] ARRAY_ICONST_1 = new byte[]{ICONST_1};
    private static final byte[] ARRAY_ICONST_2 = new byte[]{ICONST_2};
    private static final byte[] ARRAY_ICONST_3 = new byte[]{ICONST_3};
    private static final byte[] ARRAY_ICONST_4 = new byte[]{ICONST_4};
    private static final byte[] ARRAY_ICONST_5 = new byte[]{ICONST_5};
    private static final byte[] ARRAY_LCONST_0 = new byte[]{LCONST_0};
    private static final byte[] ARRAY_LCONST_1 = new byte[]{LCONST_1};
    private static final byte[] ARRAY_FCONST_0 = new byte[]{FCONST_0};
    private static final byte[] ARRAY_FCONST_1 = new byte[]{FCONST_1};
    private static final byte[] ARRAY_FCONST_2 = new byte[]{FCONST_2};
    private static final byte[] ARRAY_DCONST_0 = new byte[]{DCONST_0};
    private static final byte[] ARRAY_DCONST_1 = new byte[]{DCONST_1};

    static int getTag(Object o) {
        if (o instanceof Float) {
            return ConstPool.CONST_Float;
        } else if (o instanceof Double) {
            return ConstPool.CONST_Double;
        } else if (o instanceof Integer) {
            return ConstPool.CONST_Integer;
        } else if (o instanceof Long) {
            return ConstPool.CONST_Long;
        } else if (o instanceof String) {
            return ConstPool.CONST_String;
        } else if (o instanceof JavaRef) {
            return ((JavaRef) o).getTag();
        }
        throw new IllegalArgumentException("Unhandled type: " + o.getClass().getName());
    }

    private static int addToPool(ConstPool cp, Object o) {
        if (o instanceof Float) {
            return cp.addFloatInfo((Float) o);
        } else if (o instanceof Double) {
            return cp.addDoubleInfo((Double) o);
        } else if (o instanceof Integer) {
            return cp.addIntegerInfo((Integer) o);
        } else if (o instanceof Long) {
            return cp.addLongInfo((Long) o);
        } else if (o instanceof String) {
            return cp.addStringInfo((String) o);
        } else if (o instanceof MethodRef) {
            MethodRef mr = (MethodRef) o;
            int ci = findOrAdd(cp, new ClassRef(mr.getClassName()));
            return cp.addMethodrefInfo(ci, mr.getName(), mr.getType());
        } else if (o instanceof InterfaceMethodRef) {
            InterfaceMethodRef imr = (InterfaceMethodRef) o;
            int ci = findOrAdd(cp, new ClassRef(imr.getClassName()));
            return cp.addInterfaceMethodrefInfo(ci, imr.getName(), imr.getType());
        } else if (o instanceof FieldRef) {
            FieldRef fr = (FieldRef) o;
            int ci = findOrAdd(cp, new ClassRef(fr.getClassName()));
            return cp.addFieldrefInfo(ci, fr.getName(), fr.getType());
        } else if (o instanceof ClassRef) {
            return cp.addClassInfo(((ClassRef) o).getClassName());
        }
        throw new IllegalArgumentException("Unhandled type: " + o.getClass().getName());
    }

    static boolean checkEqual(ConstPool cp, int index, int tag, Object o) {
        if (o instanceof Float) {
            return cp.getFloatInfo(index) == (Float) o;
        } else if (o instanceof Double) {
            return cp.getDoubleInfo(index) == (Double) o;
        } else if (o instanceof Integer) {
            return cp.getIntegerInfo(index) == (Integer) o;
        } else if (o instanceof Long) {
            return cp.getLongInfo(index) == (Long) o;
        } else if (o instanceof String) {
            return o.equals(cp.getStringInfo(index));
        } else if (o instanceof JavaRef) {
            JavaRef ref = (JavaRef) o;
            return ref.getTag() == tag && ref.checkEqual(cp, index);
        }
        throw new IllegalArgumentException("Unhandled type: " + o.getClass().getName());
    }

    static int find(ConstPool cp, Object value) {
        return find(cp, value, getTag(value));
    }

    static int find(ConstPool cp, Object value, int tag) {
        for (int i = 1; i < cp.getSize(); i++) {
            if (cp.getTag(i) == tag && checkEqual(cp, i, tag, value)) {
                return i;
            }
        }
        return -1;
    }

    private static int findOrAdd(ConstPool cp, Object value) {
        int index = find(cp, value);
        if (index < 0) {
            index = addToPool(cp, value);
        }
        return index;
    }

    private static byte[] getLoad(int op, int i) {
        int mop = op;
        if (i > MAX_LDC_INDEX && mop == LDC) {
            mop = LDC_W;
        }
        if (mop == LDC) {
            return new byte[]{(byte) mop, Util.b(i, 0)};
        } else {
            return new byte[]{(byte) mop, Util.b(i, 1), Util.b(i, 0)};
        }
    }

    private static Object getLoadExpr(int op, int i) {
        int mop = op;
        if (i > MAX_LDC_INDEX && mop == LDC) {
            mop = LDC_W;
        }
        if (mop == LDC) {
            // obfuscator occasionally uses LDC_W even when LDC would do, so we're forced to generate a regex here
            return BinaryRegex.or(
                BinaryRegex.build(LDC, Util.b(i, 0)),
                BinaryRegex.build(LDC_W, Util.b(i, 1), Util.b(i, 0))
            );
        } else {
            return new byte[]{(byte) mop, Util.b(i, 1), Util.b(i, 0)};
        }
    }

    static Object push(ConstPool cp, Object value, boolean add) {
        if (value == null) {
            return ARRAY_ACONST_NULL;
        } else if (value instanceof Boolean) {
            if ((Boolean) value) {
                return ARRAY_ICONST_1;
            } else {
                return ARRAY_ICONST_0;
            }
        } else if (value instanceof Byte) {
            return push(cp, (int) (Byte) value, add);
        } else if (value instanceof Character) {
            return push(cp, (int) (Character) value, add);
        } else if (value instanceof Integer) {
            int i = (Integer) value;
            switch (i) {
                case -1:
                    return ARRAY_ICONST_M1;
                case 0:
                    return ARRAY_ICONST_0;
                case 1:
                    return ARRAY_ICONST_1;
                case 2:
                    return ARRAY_ICONST_2;
                case 3:
                    return ARRAY_ICONST_3;
                case 4:
                    return ARRAY_ICONST_4;
                case 5:
                    return ARRAY_ICONST_5;
                default:
                    break;
            }
            if (i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE) {
                return new byte[]{BIPUSH, (byte) i};
            } else if (i >= Short.MIN_VALUE && i <= Short.MAX_VALUE) {
                return new byte[]{SIPUSH, Util.b(i, 1), Util.b(i, 0)};
            }
        } else if (value instanceof Long) {
            long l = (Long) value;
            if (l == 0L) {
                return ARRAY_LCONST_0;
            } else if (l == 1L) {
                return ARRAY_LCONST_1;
            }
        } else if (value instanceof Float) {
            float f = (Float) value;
            if (f == 0.0F) {
                return ARRAY_FCONST_0;
            } else if (f == 1.0F) {
                return ARRAY_FCONST_1;
            } else if (f == 2.0F) {
                return ARRAY_FCONST_2;
            }
        } else if (value instanceof Double) {
            double d = (Double) value;
            if (d == 0.0) {
                return ARRAY_DCONST_0;
            } else if (d == 1.0) {
                return ARRAY_DCONST_1;
            }
        }
        int index = add ? findOrAdd(cp, value) : find(cp, value);
        if (index >= 0) {
            int op = (value instanceof Double || value instanceof Long ? LDC2_W : LDC);
            if (add) {
                return getLoad(op, index);
            } else {
                return getLoadExpr(op, index);
            }
        } else {
            return NOT_FOUND;
        }
    }

    static byte[] reference(ConstPool cp, Object value, boolean add) {
        int index = add ? findOrAdd(cp, value) : find(cp, value);
        if (index < 0) {
            return NOT_FOUND;
        }
        return Util.marshal16(index);
    }

    static void matchOpcodeToRefType(int opcode, Object value) {
        opcode &= 0xff;
        if (Util.contains(CONSTANT_OPCODES, opcode)) {
        } else if (Util.contains(FIELDREF_OPCODES, opcode)) {
            if (!(value instanceof FieldRef)) {
                throw new IllegalArgumentException(Mnemonic.OPCODE[opcode] + " requires a FieldRef object");
            }
        } else if (Util.contains(CLASSREF_OPCODES, opcode)) {
            if (!(value instanceof ClassRef)) {
                throw new IllegalArgumentException(Mnemonic.OPCODE[opcode] + " requires a ClassRef object");
            }
        } else if (Util.contains(METHODREF_OPCODES, opcode)) {
            if (!(value instanceof MethodRef)) {
                throw new IllegalArgumentException(Mnemonic.OPCODE[opcode] + " requires a MethodRef object");
            }
        } else if (Util.contains(INTERFACEMETHODREF_OPCODES, opcode)) {
            if (!(value instanceof InterfaceMethodRef)) {
                throw new IllegalArgumentException(Mnemonic.OPCODE[opcode] + " requires an InterfaceMethodRef object");
            }
        }
    }

    static void matchConstPoolTagToRefType(int tag, Object value) {
        switch (tag) {
            case ConstPool.CONST_Fieldref:
                if (!(value instanceof FieldRef)) {
                    throw new IllegalArgumentException("CONST_Fieldref requires a FieldRef object");
                }
                break;

            case ConstPool.CONST_Methodref:
                if (!(value instanceof MethodRef)) {
                    throw new IllegalArgumentException("CONST_Methodref requires a MethodRef object");
                }
                break;

            case ConstPool.CONST_InterfaceMethodref:
                if (!(value instanceof InterfaceMethodRef)) {
                    throw new IllegalArgumentException("CONST_InterfaceMethodref requires an InterfaceMethodRef object");
                }
                break;

            case ConstPool.CONST_Class:
                if (!(value instanceof ClassRef)) {
                    throw new IllegalArgumentException("CONST_Class requires a ClassRef object");
                }
                break;

            default:
                break;
        }
    }

    static JavaRef getRefForIndex(ConstPool constPool, int index) {
        int tag = constPool.getTag(index);
        switch (tag) {
            case ConstPool.CONST_Fieldref:
                return new FieldRef(
                    constPool.getFieldrefClassName(index),
                    constPool.getFieldrefName(index),
                    constPool.getFieldrefType(index)
                );

            case ConstPool.CONST_Methodref:
                return new MethodRef(
                    constPool.getMethodrefClassName(index),
                    constPool.getMethodrefName(index),
                    constPool.getMethodrefType(index)
                );

            case ConstPool.CONST_InterfaceMethodref:
                return new InterfaceMethodRef(
                    constPool.getInterfaceMethodrefClassName(index),
                    constPool.getInterfaceMethodrefName(index),
                    constPool.getInterfaceMethodrefType(index)
                );

            case ConstPool.CONST_Class:
                return new ClassRef(constPool.getClassInfo(index));

            default:
                throw new IllegalArgumentException(
                    String.format("cannot create JavaRef for constPool[%d] tag %d", index, tag)
                );
        }
    }

    static byte[] reference(ConstPool cp, int opcode, Object value, boolean add) {
        int index = add ? findOrAdd(cp, value) : find(cp, value);
        if (index < 0) {
            return NOT_FOUND;
        }
        matchOpcodeToRefType(opcode, value);
        if (Util.contains(CONSTANT_OPCODES, opcode)) {
            return getLoad(opcode, index);
        } else if (Util.contains(INTERFACEMETHODREF_OPCODES, opcode)) {
            if (value instanceof InterfaceMethodRef) {
                int numArgs = 0;
                for (String s : parseDescriptor(((InterfaceMethodRef) value).getType())) {
                    numArgs += (s.equals("D") || s.equals("J") ? 2 : 1);
                }
                return new byte[]{(byte) opcode, Util.b(index, 1), Util.b(index, 0), (byte) numArgs, 0};
            }
        }
        return new byte[]{(byte) opcode, Util.b(index, 1), Util.b(index, 0)};
    }

    static void checkTypeDescriptorSyntax(String descriptor) {
        for (int i = 0; i < descriptor.length(); i++) {
            char c = descriptor.charAt(i);
            if (DESCRIPTOR_CHARS.indexOf(c) >= 0) {
            } else if (c == 'L') {
                int j = descriptor.indexOf(';', i);
                if (j < 0) {
                    throw new IllegalArgumentException("invalid type descriptor \"" + descriptor + "\": missing semicolon @" + i);
                }
                i = j;
            } else {
                throw new IllegalArgumentException("invalid type descriptor \"" + descriptor + "\": bad type @" + i);
            }
        }
    }

    static ArrayList<String> parseDescriptor(String descriptor) {
        checkTypeDescriptorSyntax(descriptor);
        ArrayList<String> types = new ArrayList<String>();
        descriptor = descriptor.replaceAll("[()]", "");
        int len = descriptor.length();
        int j;
        for (int i = 0; i < len; i = j + 1) {
            for (j = i; j < len; j++) {
                char c = descriptor.charAt(j);
                if (DESCRIPTOR_TYPES.indexOf(c) >= 0) {
                    break;
                } else if (c == 'L') {
                    while (descriptor.charAt(j) != ';') {
                        j++;
                    }
                    break;
                }
            }
            types.add(descriptor.substring(i, j + 1));
        }
        return types;
    }
}
