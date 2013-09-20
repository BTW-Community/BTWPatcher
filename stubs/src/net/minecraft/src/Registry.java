package net.minecraft.src;

import java.util.Iterator;
import java.util.List;

public class Registry<T> extends RegistryBase<String, T> implements RegistryIterable<T> {
    public void register(int id, String name, T object) {
    }

    public String getFullName(String name) {
        return name;
    }

    public int getId(T object) {
        return 0;
    }

    public T getById(int id) {
        return null;
    }

    // 13w38a
    @Deprecated
    public List<T> getAll() {
        return null;
    }

    // 13w38b+
    @Override
    public Iterator<T> iterator() {
        return null;
    }
}
