package com.prupe.mcpatcher;

import javassist.bytecode.*;

import java.util.BitSet;
import java.util.List;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static javassist.bytecode.Opcode.*;

/**
 * BinaryMatcher used with Java bytecode.
 */
public class BytecodeMatcher extends BinaryMatcher {
    /**
     * Fixed regex that matches any ALOAD ... instruction.
     */
    public static final String anyALOAD = or(
        build(ALOAD_0),
        build(ALOAD_1),
        build(ALOAD_2),
        build(ALOAD_3),
        build(ALOAD, any()),
        build(WIDE, ALOAD, any(2))
    );

    /**
     * Fixed regex that matches any ASTORE ... instruction.
     */
    public static final String anyASTORE = or(
        build(ASTORE_0),
        build(ASTORE_1),
        build(ASTORE_2),
        build(ASTORE_3),
        build(ASTORE, any()),
        build(WIDE, ASTORE, any(2))
    );

    /**
     * Fixed regex that matches any ILOAD ... instruction.
     */
    public static final String anyILOAD = or(
        build(ILOAD_0),
        build(ILOAD_1),
        build(ILOAD_2),
        build(ILOAD_3),
        build(ILOAD, any()),
        build(WIDE, ILOAD, any(2))
    );

    /**
     * Fixed regex that matches any ISTORE ... instruction.
     */
    public static final String anyISTORE = or(
        build(ISTORE_0),
        build(ISTORE_1),
        build(ISTORE_2),
        build(ISTORE_3),
        build(ISTORE, any()),
        build(WIDE, ISTORE, any(2))
    );

    /**
     * Fixed regex that matches any LLOAD ... instruction.
     */
    public static final String anyLLOAD = or(
        build(LLOAD_0),
        build(LLOAD_1),
        build(LLOAD_2),
        build(LLOAD_3),
        build(LLOAD, any()),
        build(WIDE, LLOAD, any(2))
    );

    /**
     * Fixed regex that matches any LSTORE ... instruction.
     */
    public static final String anyLSTORE = or(
        build(LSTORE_0),
        build(LSTORE_1),
        build(LSTORE_2),
        build(LSTORE_3),
        build(LSTORE, any()),
        build(WIDE, LSTORE, any(2))
    );

    /**
     * Fixed regex that matches any FLOAD ... instruction.
     */
    public static final String anyFLOAD = or(
        build(FLOAD_0),
        build(FLOAD_1),
        build(FLOAD_2),
        build(FLOAD_3),
        build(FLOAD, any()),
        build(WIDE, FLOAD, any(2))
    );

    /**
     * Fixed regex that matches any FSTORE ... instruction.
     */
    public static final String anyFSTORE = or(
        build(FSTORE_0),
        build(FSTORE_1),
        build(FSTORE_2),
        build(FSTORE_3),
        build(FSTORE, any()),
        build(WIDE, FSTORE, any(2))
    );

    /**
     * Fixed regex that matches any DLOAD ... instruction.
     */
    public static final String anyDLOAD = or(
        build(DLOAD_0),
        build(DLOAD_1),
        build(DLOAD_2),
        build(DLOAD_3),
        build(DLOAD, any()),
        build(WIDE, DLOAD, any(2))
    );

    /**
     * Fixed regex that matches any DSTORE ... instruction.
     */
    public static final String anyDSTORE = or(
        build(DSTORE_0),
        build(DSTORE_1),
        build(DSTORE_2),
        build(DSTORE_3),
        build(DSTORE, any()),
        build(WIDE, DSTORE, any(2))
    );

    /**
     * Fixed regex that matches any LDC/LDC_W ... instruction.
     */
    public static final String anyLDC = or(
        build(LDC, any()),
        build(LDC_W, any(2)),
        build(LDC2_W, any(2))
    );

    /**
     * Fixed regexes that match pairs of branch opcodes.  Useful for matching either<br/>
     * if (a > b) { foo(); } else { bar(); }<br/>
     * or<br/>
     * if (a <= b) { bar(); } else { foo(); }
     */
    public static final String IFEQ_or_IFNE = subset(new int[]{IFEQ, IFNE}, true);
    public static final String IFNE_or_IFEQ = IFEQ_or_IFNE;

    public static final String IFGE_or_IFLT = subset(new int[]{IFGE, IFLT}, true);
    public static final String IFLT_or_IFGE = IFGE_or_IFLT;

    public static final String IFLE_or_IFGT = subset(new int[]{IFLE, IFGT}, true);
    public static final String IFGT_or_IFLE = IFLE_or_IFGT;

    public static final String IFNULL_or_IFNONNULL = subset(new int[]{IFNULL, IFNONNULL}, true);
    public static final String IFNONNULL_or_IFNULL = IFNULL_or_IFNONNULL;

    public static final String IF_ACMPEQ_or_IF_ACMPNE = subset(new int[]{IF_ACMPEQ, IF_ACMPNE}, true);
    public static final String IF_ACMPNE_or_IF_ACMPEQ = IF_ACMPEQ_or_IF_ACMPNE;

    public static final String IF_ICMPEQ_or_IF_ICMPNE = subset(new int[]{IF_ICMPEQ, IF_ICMPNE}, true);
    public static final String IF_ICMPNE_or_IF_ICMPEQ = IF_ICMPEQ_or_IF_ICMPNE;

    public static final String IF_ICMPGE_or_IF_ICMPLT = subset(new int[]{IF_ICMPGE, IF_ICMPLT}, true);
    public static final String IF_ICMPLT_or_IF_ICMPGE = IF_ICMPGE_or_IF_ICMPLT;

    public static final String IF_ICMPLE_or_IF_ICMPGT = subset(new int[]{IF_ICMPLE, IF_ICMPGT}, true);
    public static final String IF_ICMPGT_or_IF_ICMPLE = IF_ICMPLE_or_IF_ICMPGT;

    private static final int[] ALOAD_OPCODES = {ALOAD_0, ALOAD_1, ALOAD_2, ALOAD_3};
    private static final int[] ASTORE_OPCODES = {ASTORE_0, ASTORE_1, ASTORE_2, ASTORE_3};
    private static final int[] ILOAD_OPCODES = {ILOAD_0, ILOAD_1, ILOAD_2, ILOAD_3};
    private static final int[] ISTORE_OPCODES = {ISTORE_0, ISTORE_1, ISTORE_2, ISTORE_3};
    private static final int[] LLOAD_OPCODES = {LLOAD_0, LLOAD_1, LLOAD_2, LLOAD_3};
    private static final int[] LSTORE_OPCODES = {LSTORE_0, LSTORE_1, LSTORE_2, LSTORE_3};
    private static final int[] FLOAD_OPCODES = {FLOAD_0, FLOAD_1, FLOAD_2, FLOAD_3};
    private static final int[] FSTORE_OPCODES = {FSTORE_0, FSTORE_1, FSTORE_2, FSTORE_3};
    private static final int[] DLOAD_OPCODES = {DLOAD_0, DLOAD_1, DLOAD_2, DLOAD_3};
    private static final int[] DSTORE_OPCODES = {DSTORE_0, DSTORE_1, DSTORE_2, DSTORE_3};

    private static int getOpcodeByteCount(int opcode) {
        opcode &= 0xff;
        switch (opcode) {
            case LDC:
                return 1;

            case LDC_W:
            case LDC2_W:
            case NEW:
            case ANEWARRAY:
            case CHECKCAST:
            case INSTANCEOF:
            case GETSTATIC:
            case PUTSTATIC:
            case GETFIELD:
            case PUTFIELD:
            case INVOKESPECIAL:
            case INVOKEVIRTUAL:
            case INVOKESTATIC:
                return 2;

            case MULTIANEWARRAY:
                return 3;

            case INVOKEINTERFACE:
            case INVOKEDYNAMIC:
                return 4;

            default:
                throw new IllegalArgumentException("opcode " + Mnemonic.OPCODE[opcode]);
        }
    }

    private static String toRegex(BitSet bits) {
        if (bits.isEmpty()) {
            return null;
        } else {
            int[] ints = new int[bits.cardinality()];
            int i = 0;
            for (int bit = bits.nextSetBit(0); bit >= 0; bit = bits.nextSetBit(bit + 1)) {
                ints[i++] = bit;
            }
            return subset(ints, true);
        }
    }

    /**
     * Regex that matches any opcode+16-bit const pool index
     *
     * @param opcode opcode, e.g., INVOKEVIRTUAL
     * @return regex
     */
    public static String anyReference(int opcode) {
        return build(opcode, any(getOpcodeByteCount(opcode)));
    }

    /**
     * Regex that matches any opcode+16-bit const pool index
     *
     * @param opcodes list of opcodes, e.g., INVOKEVIRTUAL, INVOKESPECIAL
     * @return regex
     */
    public static String anyReference(int... opcodes) {
        BitSet[] bits = new BitSet[5];
        for (int i = 1; i < bits.length; i++) {
            bits[i] = new BitSet();
        }
        for (int opcode : opcodes) {
            opcode &= 0xff;
            bits[getOpcodeByteCount(opcode)].set(opcode);
        }
        String[] expr = new String[5];
        for (int i = 1; i < bits.length; i++) {
            expr[i] = build(toRegex(bits[i]), any(i));
        }
        return or(expr);
    }

    /**
     * Convenience method for capture(anyReference(...))
     *
     * @param opcode opcode, e.g., INVOKEVIRTUAL
     * @return regex
     */
    public static String captureReference(int opcode) {
        return capture(anyReference(opcode));
    }

    /**
     * Convenience method for capture(anyReference(...))
     *
     * @param opcodes list of opcodes, e.g., INVOKEVIRTUAL, INVOKESPECIAL
     * @return regex
     */
    public static String captureReference(int... opcodes) {
        return capture(anyReference(opcodes));
    }

    private static byte[] registerLoadStore(int[] x, int y, int register) {
        if (register < 0 || register > 255) {
            throw new IllegalArgumentException("invalid register " + register);
        } else if (register < x.length) {
            return new byte[]{(byte) x[register]};
        } else {
            return new byte[]{(byte) y, (byte) register};
        }
    }

    /**
     * Returns bytecode to load/store the given register.  Useful when the register number
     * itself is not constant.
     *
     * @param opcode   base opcode: ALOAD, ASTORE, etc.
     * @param register register number
     * @return bytecode
     */
    public static byte[] registerLoadStore(int opcode, int register) {
        opcode &= 0xff;
        switch (opcode) {
            case ALOAD:
                return registerLoadStore(ALOAD_OPCODES, ALOAD, register);

            case ASTORE:
                return registerLoadStore(ASTORE_OPCODES, ASTORE, register);

            case ILOAD:
                return registerLoadStore(ILOAD_OPCODES, ILOAD, register);

            case ISTORE:
                return registerLoadStore(ISTORE_OPCODES, ISTORE, register);

            case LLOAD:
                return registerLoadStore(LLOAD_OPCODES, LLOAD, register);

            case LSTORE:
                return registerLoadStore(LSTORE_OPCODES, LSTORE, register);

            case FLOAD:
                return registerLoadStore(FLOAD_OPCODES, FLOAD, register);

            case FSTORE:
                return registerLoadStore(FSTORE_OPCODES, FSTORE, register);

            case DLOAD:
                return registerLoadStore(DLOAD_OPCODES, DLOAD, register);

            case DSTORE:
                return registerLoadStore(DSTORE_OPCODES, DSTORE, register);

            default:
                throw new IllegalArgumentException("invalid opcode " + Mnemonic.OPCODE[opcode]);
        }
    }

    /**
     * "Flips" a register load/store operation to its opposite: e.g., ALOAD_1 -> ASTORE_1; ISTORE, 5 -> ILOAD, 5.
     *
     * @param code load/store operation
     * @return matching store/load operation of the same register and type
     */
    public static byte[] flipLoadStore(byte[] code) {
        if (code == null) {
            return null;
        }
        byte[] newcode = code.clone();
        int offset = 0;
        if (newcode[offset] == WIDE) {
            offset++;
        }
        int opcode = newcode[offset] & 0xff;
        switch (opcode) {
            case ALOAD:
                opcode = ASTORE;
                break;

            case ALOAD_0:
                opcode = ASTORE_0;
                break;

            case ALOAD_1:
                opcode = ASTORE_1;
                break;

            case ALOAD_2:
                opcode = ASTORE_2;
                break;

            case ALOAD_3:
                opcode = ASTORE_3;
                break;

            case ASTORE:
                opcode = ALOAD;
                break;

            case ASTORE_0:
                opcode = ALOAD_0;
                break;

            case ASTORE_1:
                opcode = ALOAD_1;
                break;

            case ASTORE_2:
                opcode = ALOAD_2;
                break;

            case ASTORE_3:
                opcode = ALOAD_3;
                break;

            case ILOAD:
                opcode = ISTORE;
                break;

            case ILOAD_0:
                opcode = ISTORE_0;
                break;

            case ILOAD_1:
                opcode = ISTORE_1;
                break;

            case ILOAD_2:
                opcode = ISTORE_2;
                break;

            case ILOAD_3:
                opcode = ISTORE_3;
                break;

            case ISTORE:
                opcode = ILOAD;
                break;

            case ISTORE_0:
                opcode = ILOAD_0;
                break;

            case ISTORE_1:
                opcode = ILOAD_1;
                break;

            case ISTORE_2:
                opcode = ILOAD_2;
                break;

            case ISTORE_3:
                opcode = ILOAD_3;
                break;

            case LLOAD:
                opcode = LSTORE;
                break;

            case LLOAD_0:
                opcode = LSTORE_0;
                break;

            case LLOAD_1:
                opcode = LSTORE_1;
                break;

            case LLOAD_2:
                opcode = LSTORE_2;
                break;

            case LLOAD_3:
                opcode = LSTORE_3;
                break;

            case LSTORE:
                opcode = LLOAD;
                break;

            case LSTORE_0:
                opcode = LLOAD_0;
                break;

            case LSTORE_1:
                opcode = LLOAD_1;
                break;

            case LSTORE_2:
                opcode = LLOAD_2;
                break;

            case LSTORE_3:
                opcode = LLOAD_3;
                break;

            case FLOAD:
                opcode = FSTORE;
                break;

            case FLOAD_0:
                opcode = FSTORE_0;
                break;

            case FLOAD_1:
                opcode = FSTORE_1;
                break;

            case FLOAD_2:
                opcode = FSTORE_2;
                break;

            case FLOAD_3:
                opcode = FSTORE_3;
                break;

            case FSTORE:
                opcode = FLOAD;
                break;

            case FSTORE_0:
                opcode = FLOAD_0;
                break;

            case FSTORE_1:
                opcode = FLOAD_1;
                break;

            case FSTORE_2:
                opcode = FLOAD_2;
                break;

            case FSTORE_3:
                opcode = FLOAD_3;
                break;

            case DLOAD:
                opcode = DSTORE;
                break;

            case DLOAD_0:
                opcode = DSTORE_0;
                break;

            case DLOAD_1:
                opcode = DSTORE_1;
                break;

            case DLOAD_2:
                opcode = DSTORE_2;
                break;

            case DLOAD_3:
                opcode = DSTORE_3;
                break;

            case DSTORE:
                opcode = DLOAD;
                break;

            case DSTORE_0:
                opcode = DLOAD_0;
                break;

            case DSTORE_1:
                opcode = DLOAD_1;
                break;

            case DSTORE_2:
                opcode = DLOAD_2;
                break;

            case DSTORE_3:
                opcode = DLOAD_3;
                break;

            default:
                throw new IllegalArgumentException("invalid opcode " + Mnemonic.OPCODE[opcode]);
        }
        newcode[offset] = (byte) opcode;
        return newcode;
    }

    public static int extractRegisterNum(byte[] code) {
        switch (code[0] & 0xff) {
            case ALOAD_0:
            case ASTORE_0:
            case ILOAD_0:
            case ISTORE_0:
            case LLOAD_0:
            case LSTORE_0:
            case FLOAD_0:
            case FSTORE_0:
            case DLOAD_0:
            case DSTORE_0:
                return 0;

            case ALOAD_1:
            case ASTORE_1:
            case ILOAD_1:
            case ISTORE_1:
            case LLOAD_1:
            case LSTORE_1:
            case FLOAD_1:
            case FSTORE_1:
            case DLOAD_1:
            case DSTORE_1:
                return 1;

            case ALOAD_2:
            case ASTORE_2:
            case ILOAD_2:
            case ISTORE_2:
            case LLOAD_2:
            case LSTORE_2:
            case FLOAD_2:
            case FSTORE_2:
            case DLOAD_2:
            case DSTORE_2:
                return 2;

            case ALOAD_3:
            case ASTORE_3:
            case ILOAD_3:
            case ISTORE_3:
            case LLOAD_3:
            case LSTORE_3:
            case FLOAD_3:
            case FSTORE_3:
            case DLOAD_3:
            case DSTORE_3:
                return 3;

            case ALOAD:
            case ASTORE:
            case ILOAD:
            case ISTORE:
            case LLOAD:
            case LSTORE:
            case FLOAD:
            case FSTORE:
            case DLOAD:
            case DSTORE:
                return code[1] & 0xff;

            case WIDE:
                switch (code[1] & 0xff) {
                    case ALOAD:
                    case ASTORE:
                    case ILOAD:
                    case ISTORE:
                    case LLOAD:
                    case LSTORE:
                    case FLOAD:
                    case FSTORE:
                    case DLOAD:
                    case DSTORE:
                        return ((code[2] & 0xff) << 8) | (code[3] & 0xff);

                    default:
                        throw new IllegalArgumentException("invalid opcode WIDE " + Mnemonic.OPCODE[code[1]]);
                }

            default:
                throw new IllegalArgumentException("invalid opcode " + Mnemonic.OPCODE[code[0]]);
        }
    }

    public static int extractConstPoolIndex(byte[] code) {
        switch (code[0]) {
            case LDC:
                return Util.demarshal(code, 1, 1);

            default:
                return Util.demarshal(code, 1, 2);
        }
    }

    public static String parseDescriptor(String descriptor, boolean isStatic, List<String> types, List<Integer> registers) {
        int register = 0;
        if (!isStatic) {
            types.add("L...;");
            registers.add(register++);
        }
        List<String> parsed = ConstPoolUtils.parseDescriptor(descriptor);
        String returnType = parsed.remove(parsed.size() - 1);
        for (String s : parsed) {
            types.add(s);
            registers.add(register);
            if (s.equals("D") || s.equals("J")) {
                register += 2;
            } else {
                register++;
            }
        }
        return returnType;
    }

    /**
     * Construct a new matcher for the given regular expression.
     *
     * @param objects BinaryRegex elements that make up the expression to match
     * @see BinaryRegex#build(Object...)
     */
    public BytecodeMatcher(Object... objects) {
        super(objects);
    }

    /**
     * Match expression against any method in a class file.
     *
     * @param classFile class file to match
     * @return true if any method matches
     */
    public boolean match(ClassFile classFile) {
        for (Object o : classFile.getMethods()) {
            MethodInfo mi = (MethodInfo) o;
            if (match(mi)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Match expression against a single method in a class file.
     *
     * @param methodInfo method to match
     * @return true if match
     */
    public boolean match(MethodInfo methodInfo) {
        return match(methodInfo, 0);
    }

    /**
     * Match expression against a single method in a class file, starting at a particular offset.
     *
     * @param methodInfo method to match
     * @param offset     position in bytecode at which to start looking for matches.
     * @return true if match
     */
    public boolean match(MethodInfo methodInfo, int offset) {
        CodeAttribute ca = methodInfo.getCodeAttribute();
        CodeIterator ci = ca.iterator();
        byte[] code = ca.getCode();
        int next;

        while (offset < code.length) {
            if (!match(code, offset)) {
                break;
            }
            try {
                for (next = offset; next < getStart() && ci.hasNext(); next = ci.next())
                    ;
            } catch (BadBytecode e) {
                Logger.log(e);
                break;
            }
            if (next == getStart()) {
                return true;
            }
            offset = next;
        }

        return false;
    }
}
