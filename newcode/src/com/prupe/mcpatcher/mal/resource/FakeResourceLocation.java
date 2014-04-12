package com.prupe.mcpatcher.mal.resource;

final public class FakeResourceLocation {
    public static final String FIXED_NAMESPACE = TexturePackAPI.DEFAULT_NAMESPACE;

    private final String path;
    private final String toString;
    private final int hashCode;

    public static FakeResourceLocation wrap(String path) {
        return path == null ? null : new FakeResourceLocation(path);
    }

    public static String unwrap(FakeResourceLocation resourceLocation) {
        return resourceLocation == null ? null : resourceLocation.getPath();
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
}
