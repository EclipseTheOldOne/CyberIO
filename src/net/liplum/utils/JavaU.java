package net.liplum.utils;

import com.google.common.collect.ObjectArrays;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;

@SuppressWarnings("unchecked")
public class JavaU {
    private static final HashMap<Class<?>, Object[]> EmptyArrays = new HashMap<>();

    @NotNull
    public static <T> T[] emptyArray(Class<T> clz) {
        return (T[]) EmptyArrays.computeIfAbsent(clz, (k) -> ObjectArrays.newArray(k, 0));
    }

    public static boolean equals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if ((a == null) || (b == null)) {
            return false;
        }
        return a.equals(b);
    }

    public static <T> boolean equalsNoOrder(T[] a, T[] b) {
        if (a.length == b.length) {
            if (a.length == 0) {
                return true;
            }
            return Arrays.asList(a).containsAll(Arrays.asList(b));
        }
        return false;
    }
}
