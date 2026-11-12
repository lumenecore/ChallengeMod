package com.lumenechallenge.compat;

import net.minecraft.nbt.NbtCompound;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Runtime compatibility helper for NBT method descriptor changes across supported Minecraft versions.
 */
public final class NbtCompat {
    private static final Method REMOVE_METHOD = findRemoveMethod();

    private NbtCompat() {
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
