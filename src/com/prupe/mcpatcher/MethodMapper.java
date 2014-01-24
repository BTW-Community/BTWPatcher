package com.prupe.mcpatcher;

import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;

import java.util.List;

/**
 * Represents a method to be located within a class.  By default,
 * the match is done by type signature, but this can be overridden.
 */
public class MethodMapper extends MemberMapper {
    public MethodMapper(ClassMod classMod, MethodRef... refs) {
        super(classMod, refs);
    }

    public MethodMapper mapToInterface(int mapInterface) {
        this.mapInterface = mapInterface;
        return this;
    }

    protected final String getMapperType() {
        return "method";
    }

    protected boolean match(Object o) {
        MethodInfo methodInfo = (MethodInfo) o;
        return !methodInfo.isConstructor() && !methodInfo.isStaticInitializer() &&
            matchInfo(methodInfo.getDescriptor(), methodInfo.getAccessFlags());
    }

    protected JavaRef getObfRef(String className, Object o) {
        MethodInfo methodInfo = (MethodInfo) o;
        return new MethodRef(className, methodInfo.getName(), methodInfo.getDescriptor());
    }

    protected String[] describeMatch(Object o) {
        MethodInfo methodInfo = (MethodInfo) o;
        return new String[]{methodInfo.getName(), methodInfo.getDescriptor()};
    }

    protected List getMatchingObjects(ClassFile classFile) {
        return classFile.getMethods();
    }
}
