package net.minecraft.src;

import java.util.List;

public class Registry<T> extends RegistryBase<String, T> {
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

    public List<T> getAll() {
        return null;
    }
}
