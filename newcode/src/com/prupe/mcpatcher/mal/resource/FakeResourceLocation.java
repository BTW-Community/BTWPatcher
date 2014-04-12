package com.prupe.mcpatcher.mal.resource;

import com.prupe.mcpatcher.MCPatcherUtils;

final public class FakeResourceLocation {
    static final String FIXED_NAMESPACE = TexturePackAPI.DEFAULT_NAMESPACE;
    private static final String GRID = "##";
    private static final String BLUR = "%blur%";
    private static final String CLAMP = "%clamp%";

    private final String path;
    private final boolean grid;
    private final boolean blur;
    private final boolean clamp;
    private final String toString;
    private final int hashCode;

    public static FakeResourceLocation wrap(String path) {
        return MCPatcherUtils.isNullOrEmpty(path) ? null : new FakeResourceLocation(path);
    }

    public static String unwrap(FakeResourceLocation resourceLocation) {
        return resourceLocation == null ? null : resourceLocation.getPath();
    }

    public FakeResourceLocation(String namespace, String path) {
        if ((grid = path.startsWith(GRID))) {
            path = path.substring(GRID.length());
        }
        if ((blur = path.startsWith(BLUR))) {
            path = path.substring(BLUR.length());
        }
        if ((clamp = path.startsWith(CLAMP))) {
            path = path.substring(CLAMP.length());
        }
        this.path = path.startsWith("/") ? path : "/" + path;
        toString = (grid ? GRID : "") + (blur ? BLUR : "") + (clamp ? CLAMP : "") + path;
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

    public boolean isGrid() {
        return grid;
    }

    public boolean isBlur() {
        return blur;
    }

    public boolean isClamp() {
        return clamp;
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
