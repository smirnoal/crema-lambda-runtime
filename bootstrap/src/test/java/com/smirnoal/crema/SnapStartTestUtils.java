package com.smirnoal.crema;

import java.lang.reflect.Field;
import java.util.List;

final class SnapStartTestUtils {
    private static final String ORIGINAL_INIT_TYPE = Lambda.Environment.AWS_LAMBDA_INITIALIZATION_TYPE;

    private SnapStartTestUtils() {
    }

    static void reset() throws Exception {
        ((List<?>) getSnapStartField("BEFORE_SNAPSHOT").get(null)).clear();
        ((List<?>) getSnapStartField("AFTER_RESTORE").get(null)).clear();
        LambdaTestUtils.setInitializationType(ORIGINAL_INIT_TYPE);
    }

    private static Field getSnapStartField(String name) throws Exception {
        Class<?> snapStartClass = Class.forName("com.smirnoal.crema.Lambda$SnapStart");
        Field field = snapStartClass.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}

