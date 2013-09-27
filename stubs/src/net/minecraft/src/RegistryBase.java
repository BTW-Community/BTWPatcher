package net.minecraft.src;

import java.util.Set;

public class RegistryBase<K, V> {
    public V getValue(K key) {
        return null;
    }

    public boolean containsKey(K key) {
        return false;
    }

    public Set<K> getKeys() {
        return null;
    }
}
