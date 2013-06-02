package com.prupe.mcpatcher;

import javassist.bytecode.ClassFile;
import javassist.bytecode.FieldInfo;

import java.util.List;

/**
 * Represents a field to be located within a class.  By default,
 * the match is done by type signature, but this can be overridden.
 */
public class FieldMapper extends MemberMapper {
    public FieldMapper(ClassMod classMod, FieldRef... refs) {
        super(classMod, refs);
    }

    protected final String getMapperType() {
        return "field";
    }

    protected boolean match(Object o) {
        FieldInfo fieldInfo = (FieldInfo) o;
        return matchInfo(fieldInfo.getDescriptor(), fieldInfo.getAccessFlags());
    }

    protected JavaRef getObfRef(String className, Object o) {
        FieldInfo fieldInfo = (FieldInfo) o;
        return new FieldRef(className, fieldInfo.getName(), fieldInfo.getDescriptor());
    }

    protected String[] describeMatch(Object o) {
        FieldInfo fieldInfo = (FieldInfo) o;
        return new String[]{fieldInfo.getName(), fieldInfo.getDescriptor()};
    }

    protected List getMatchingObjects(ClassFile classFile) {
        return classFile.getFields();
    }
}
