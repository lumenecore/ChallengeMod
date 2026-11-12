package com.lumenechallenge.compat;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Runtime compatibility helper for NBT method descriptor changes across supported Minecraft versions.
 */
public final class NbtCompat {
    private static final Method REMOVE_METHOD = findRemoveMethod();

    private NbtCompat() {
    }

    public static String getString(NbtCompound compound, String key, String fallback) {
        return compound.contains(key, NbtElement.STRING_TYPE) ? compound.getString(key) : fallback;
    }

    public static String getRequiredString(NbtCompound compound, String key) {
        if (!compound.contains(key, NbtElement.STRING_TYPE)) {
            throw new IllegalArgumentException("Missing NBT string: " + key);
        }
        return compound.getString(key);
    }

    public static int getInt(NbtCompound compound, String key, int fallback) {
        return compound.contains(key, NbtElement.NUMBER_TYPE) ? compound.getInt(key) : fallback;
    }

    public static long getLong(NbtCompound compound, String key, long fallback) {
        return compound.contains(key, NbtElement.NUMBER_TYPE) ? compound.getLong(key) : fallback;
    }

    public static float getFloat(NbtCompound compound, String key, float fallback) {
        return compound.contains(key, NbtElement.NUMBER_TYPE) ? compound.getFloat(key) : fallback;
    }

    public static double getDouble(NbtCompound compound, String key, double fallback) {
        return compound.contains(key, NbtElement.NUMBER_TYPE) ? compound.getDouble(key) : fallback;
    }

    public static boolean getBoolean(NbtCompound compound, String key, boolean fallback) {
        return compound.contains(key, NbtElement.BYTE_TYPE) ? compound.getBoolean(key) : fallback;
    }

    public static NbtList getList(NbtCompound compound, String key, int heldType) {
        return compound.getList(key, heldType);
    }

    public static NbtCompound getCompound(NbtCompound compound, String key) {
        return compound.getCompound(key);
    }

    public static NbtCompound getCompound(NbtList list, int index) {
        return list.getCompound(index);
    }

    public static int getInt(NbtList list, int index) {
        return list.getInt(index);
    }

    public static String getString(NbtList list, int index) {
        return list.getString(index);
    }

    /**
     * Removes a key without linking against the version-specific return descriptor
     * of NbtCompound.remove(String).
     *
     * The return descriptor differs between Minecraft releases. The intermediary
     * method name method_10551 is stable across this compatibility family.
     * Development environments may expose the Yarn name "remove" instead.
     */
    public static void remove(NbtCompound compound, String key) {
        if (compound == null) {
            throw new IllegalArgumentException("NBT compound must not be null");
        }
        try {
            REMOVE_METHOD.invoke(compound, key);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access NbtCompound.remove(String)", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("NbtCompound.remove(String) failed", cause);
        }
    }

    private static Method findRemoveMethod() {
        for (String methodName : new String[]{"remove", "method_10551"}) {
            try {
                return NbtCompound.class.getMethod(methodName, String.class);
            } catch (NoSuchMethodException ignored) {
                // Try the alternate name used by the other runtime namespace.
            }
        }
        throw new ExceptionInInitializerError(
                "Could not resolve NbtCompound.remove(String) using Yarn or intermediary names"
        );
    }
}
