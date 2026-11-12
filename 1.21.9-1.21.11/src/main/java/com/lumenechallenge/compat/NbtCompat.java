package com.lumenechallenge.compat;

import net.minecraft.nbt.NbtCompound;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Runtime compatibility helpers for NBT API changes between Minecraft 1.21.9-1.21.11.
 */
public final class NbtCompat {
    private static final Method REMOVE_METHOD = findRemoveMethod();

    private NbtCompat() {
    }

    /**
     * Removes a key without linking against the version-specific return descriptor
     * of NbtCompound.remove(String).
     *
     * In 1.21.9/1.21.10 the method returns void; in 1.21.11 it returns NbtElement.
     * The intermediary method name method_10551 is stable across the supported range.
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
