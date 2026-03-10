package com.smirnoal.lambda;

import com.smirnoal.lambda.Lambda.Environment;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

final class SnapStartTestHooks {
    private static final String ORIGINAL_INIT_TYPE = Environment.AWS_LAMBDA_INITIALIZATION_TYPE;
    private static final VarHandle MODIFIERS;

    static {
        try {
            MODIFIERS = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup())
                    .findVarHandle(Field.class, "modifiers", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private SnapStartTestHooks() {
    }

    @SuppressWarnings("unchecked")
    static void registerBeforeSnapshot(Runnable runnable) throws Exception {
        List<Runnable> list = (List<Runnable>) getField("BEFORE_SNAPSHOT").get(null);
        list.add(runnable);
    }

    @SuppressWarnings("unchecked")
    static void registerAfterRestore(Runnable runnable) throws Exception {
        List<Runnable> list = (List<Runnable>) getField("AFTER_RESTORE").get(null);
        list.add(runnable);
    }

    static void reset() throws Exception {
        ((List<?>) getField("BEFORE_SNAPSHOT").get(null)).clear();
        ((List<?>) getField("AFTER_RESTORE").get(null)).clear();
        setInitializationType(ORIGINAL_INIT_TYPE);
    }

    static void setInitializationType(String value) throws ReflectiveOperationException {
        Field f = Environment.class.getDeclaredField("AWS_LAMBDA_INITIALIZATION_TYPE");
        f.setAccessible(true);
        MODIFIERS.set(f, f.getModifiers() & ~Modifier.FINAL);
        f.set(null, value);
    }

    private static Field getField(String name) throws Exception {
        Class<?> snapStartClass = Class.forName("com.smirnoal.lambda.Lambda$SnapStart");
        Field field = snapStartClass.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}

