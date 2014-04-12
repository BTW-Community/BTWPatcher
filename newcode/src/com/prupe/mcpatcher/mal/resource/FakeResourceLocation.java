package com.prupe.mcpatcher.mal.resource;

final public class FakeResourceLocation {
    public static final String FIXED_NAMESPACE = "minecraft";

    private final String path;
    private final String toString;
    private final int hashCode;

    public static FakeResourceLocation wrap(String path) {
        return new FakeResourceLocation(path);
    }

    public FakeResourceLocation(String namespace, String path) {
        this.path = path.startsWith("/") ? path : "/" + path;
        toString = getNamespace() + ":" + getPath();
        hashCode = toString.hashCode();
    }

    public FakeResourceLocation(String path) {
        this(FIXED_NAMESPACE, path);
    }

    public String getNamespace() {
        return FIXED_NAMESPACE;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return toString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof FakeResourceLocation)) {
            return false;
        }
        FakeResourceLocation that = (FakeResourceLocation) o;
        return this.getPath().equals(that.getPath());
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /*
    public static String select(String v1Path, String v2Path) {
        return mal.select_Impl(v1Path, v2Path);
    }

    abstract private static class ResourceLocationMAL {
        abstract FakeResourceLocation wrap_Impl(String path);

        abstract String select_Impl(String v1Path, String v2Path);
    }

    final private static class V1 extends ResourceLocationMAL {
        @Override
        FakeResourceLocation wrap_Impl(String path) {
            return new FakeResourceLocation(path);
        }

        @Override
        String select_Impl(String v1Path, String v2Path) {
            return v1Path;
        }
    }

    final private static class V2 extends ResourceLocationMAL {
        @Override
        FakeResourceLocation wrap_Impl(String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        String select_Impl(String v1Path, String v2Path) {
            return v2Path;
        }
    }
    */
}
