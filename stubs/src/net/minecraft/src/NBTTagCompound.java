package net.minecraft.src;

import java.util.Collection;

public class NBTTagCompound extends NBTBase {
    public boolean hasKey(String tag) {
        return false;
    }

    public void removeTag(String tag) {
    }

    public Collection<NBTBase> getTags() {
        return null;
    }

    public boolean getBoolean(String tag) {
        return false;
    }

    public byte getByte(String tag) {
        return (byte) 0;
    }

    public byte[] getByteArray(String tag) {
        return null;
    }

    public NBTTagCompound getCompoundTag(String tag) {
        return null;
    }

    public NBTTagList getTagList(String tag) {
        return null;
    }

    public double getDouble(String tag) {
        return 0.0;
    }

    public float getFloat(String tag) {
        return 0.0f;
    }

    public int[] getIntArray(String tag) {
        return null;
    }

    public int getInteger(String tag) {
        return 0;
    }

    public long getLong(String tag) {
        return 0L;
    }

    public short getShort(String tag) {
        return 0;
    }

    public String getString(String tag) {
        return null;
    }

    public NBTBase getTag(String tag) {
        return null;
    }

    public void setBoolean(String tag, boolean value) {
    }

    public void setByte(String tag, byte value) {
    }

    public void setByteArray(String tag, byte[] value) {
    }

    public void setCompoundTag(String tag, NBTTagCompound value) {
    }

    public void setDouble(String tag, double value) {
    }

    public void setFloat(String tag, float value) {
    }

    public void setIntArray(String tag, int[] value) {
    }

    public void setInteger(String tag, int value) {
    }

    public void setLong(String tag, long value) {
    }

    public void setShort(String tag, short value) {
    }

    public void setString(String tag, String value) {
    }
}
