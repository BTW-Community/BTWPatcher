package com.prupe.mcpatcher;

import javassist.bytecode.*;

import java.lang.reflect.Modifier;

/**
 * ClassPatch that changes the access flags of a particular member field or method.  Default
 * behavior is to make the member public.
 */
public class MakeMemberPublicPatch extends ClassPatch {
    private final JavaRef member;
    private String type;
    private int oldFlags;
    private int newFlags;

    /**
     * @param ref may use deobfuscated names, provided they are in the class map
     */
    public MakeMemberPublicPatch(ClassMod classMod, JavaRef ref) {
        super(classMod);
        member = ref;
        optional = true;
        if (ref instanceof FieldRef) {
            type = "field";
        } else if (ref instanceof MethodRef) {
            type = "method";
        } else {
            throw new IllegalArgumentException("unexpected type: " + ref.getClass().getName());
        }
    }

    @Override
    public String getDescription() {
        int added = ~oldFlags & newFlags;
        int removed = oldFlags & ~newFlags;
        StringBuilder s = new StringBuilder();
        s.append("make ").append(type).append(" ").append(member.getName()).append(" ");
        boolean first = true;
        for (String flag : Modifier.toString(AccessFlag.toModifier(removed)).split("\\s+")) {
            if (flag.equals("")) {
                continue;
            }
            if (AccessFlag.isPublic(added) && (flag.equals("protected") || flag.equals("private"))) {
                continue;
            }
            if (AccessFlag.isProtected(added) && flag.equals("private")) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                s.append(", ");
            }
            s.append("not ").append(flag);
        }
        for (String flag : Modifier.toString(AccessFlag.toModifier(added)).split("\\s+")) {
            if (flag.equals("")) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                s.append(", ");
            }
            s.append(flag);
        }
        return s.toString();
    }

    @Override
    public boolean apply(ClassFile classFile) throws BadBytecode {
        classMod.classFile = classFile;
        classMod.methodInfo = null;
        JavaRef target = map(member);
        boolean patched = false;
        if (target instanceof FieldRef) {
            for (Object o : classFile.getFields()) {
                FieldInfo fi = (FieldInfo) o;
                if (fi.getName().equals(target.getName()) && fi.getDescriptor().equals(target.getType())) {
                    oldFlags = fi.getAccessFlags();
                    newFlags = getNewFlags(oldFlags);
                    if (oldFlags != newFlags) {
                        fi.setAccessFlags(newFlags);
                        patched = true;
                    }
                }
            }
        } else if (target instanceof MethodRef) {
            for (Object o : classFile.getMethods()) {
                MethodInfo mi = (MethodInfo) o;
                if (mi.getName().equals(target.getName()) && mi.getDescriptor().equals(target.getType())) {
                    oldFlags = mi.getAccessFlags();
                    newFlags = getNewFlags(oldFlags);
                    if (oldFlags != newFlags) {
                        mi.setAccessFlags(newFlags);
                        patched = true;
                    }
                }
            }
        }
        if (patched) {
            recordPatch();
        }
        return patched;
    }

    /**
     * Returns new set of access flags.  By default, this removes 'protected' and 'private' and adds 'public'.
     * Override this method to do something else.
     *
     * @param oldFlags old access flags
     * @return new access flags
     * @see javassist.bytecode.AccessFlag
     */
    public int getNewFlags(int oldFlags) {
        return (oldFlags & ~(AccessFlag.PRIVATE | AccessFlag.PROTECTED)) | AccessFlag.PUBLIC;
    }
}
