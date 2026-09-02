package lol.gzmc.newerinv.recipe;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class Reflect {

    private Reflect() {}

    private static final Map<String, Field> CACHE = new HashMap<String, Field>();

    public static Field field(Class<?> owner, String... names) {
        String key = owner.getName() + "#" + names[0];
        Field cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        for (String n : names) {
            try {
                Field f = owner.getDeclaredField(n);
                f.setAccessible(true);
                CACHE.put(key, f);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new RuntimeException("NewerInv: no field " + Arrays.toString(names) + " on " + owner.getName());
    }

    public static Object get(Object instance, Field field) {
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object get(Object instance, Class<?> owner, String... names) {
        return get(instance, field(owner, names));
    }
}
