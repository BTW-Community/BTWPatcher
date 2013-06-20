package com.prupe.mcpatcher;

import javassist.bytecode.*;

/**
 * ClassPatch that adds a new field to a class file.
 */
public class AddFieldPatch extends ClassPatch {
    private final FieldRef fieldRef;
    private final int accessFlags;
    private boolean allowDuplicate;

    /**
     * Add a new public, non-static field.
     *
     * @param fieldRef new field
     */
    public AddFieldPatch(ClassMod classMod, FieldRef fieldRef) {
        this(classMod, fieldRef, AccessFlag.PUBLIC);
    }

    /**
     * Add a new field.
     *
     * @param fieldRef    new field
     * @param accessFlags Java access flags (public, private, etc.).
     * @see javassist.bytecode.AccessFlag
     */
    public AddFieldPatch(ClassMod classMod, FieldRef fieldRef, int accessFlags) {
        super(classMod);
        this.fieldRef = fieldRef;
        this.accessFlags = accessFlags;
        allowDuplicate(true);
    }

    /**
     * Add a new public, non-static field.
     * NOTE: getDescriptor must be overridden if you are using this constructor.
     */
    public AddFieldPatch(ClassMod classMod, String name) {
        this(classMod, name, AccessFlag.PUBLIC);
    }

    /**
     * Add a new field.
     * NOTE: getDescriptor must be overridden if you are using this constructor.
     *
     * @param name        field name
     * @param accessFlags Java access flags (public, private, etc.).
     * @see javassist.bytecode.AccessFlag
     */
    public AddFieldPatch(ClassMod classMod, String name, int accessFlags) {
        this(classMod, new FieldRef(null, name, null), accessFlags);
    }

    /**
     * Set whether to ignore duplicate field errors when applying this patch.
     *
     * @param allowDuplicate true or false
     * @return this
     */
    public AddFieldPatch allowDuplicate(boolean allowDuplicate) {
        this.allowDuplicate = allowDuplicate;
        optional = allowDuplicate;
        return this;
    }

    /**
     * Called to get the descriptor for the new field.  Use this instead of setting the descriptor in the constructor
     * to have the field type set at patch time.
     *
     * @return descriptor
     */
    public String getDescriptor() {
        return fieldRef.getType();
    }

    @Override
    public String getDescription() {
        return String.format("insert field %s %s", fieldRef.getName(), getDescriptor());
    }

    @Override
    boolean apply(ClassFile classFile) throws BadBytecode, DuplicateMemberException {
        FieldInfo fieldInfo = new FieldInfo(classFile.getConstPool(), fieldRef.getName(), classMod.getClassMap().mapTypeString(getDescriptor()));
        fieldInfo.setAccessFlags(accessFlags);
        try {
            classFile.addField(fieldInfo);
            recordPatch();
        } catch (DuplicateMemberException e) {
            if (allowDuplicate) {
                for (Object o : classFile.getFields()) {
                    FieldInfo conflictField = (FieldInfo) o;
                    if (conflictField.getName().equals(fieldInfo.getName())) {
                        if (conflictField.getDescriptor().equals(fieldInfo.getDescriptor()) &&
                            conflictField.getAccessFlags() == fieldInfo.getAccessFlags()) {
                            return false;
                        }
                        break;
                    }
                }
                throw e;
            } else {
                throw e;
            }
        }
        return true;
    }
}
