package com.smirnoal.crema;

import com.smirnoal.crema.Lambda.Environment;
import com.smirnoal.crema.log.FormattingLogSink;
import com.smirnoal.crema.log.FramedTelemetryLogSink;

import java.io.FileOutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;

/**
 * Bootstrap test utilities: reflection-based {@link Environment} overrides and helpers for
 * logging tests (e.g. {@link #withFormattingSink(Path, FormattingSinkAction)}).
 */
public final class LambdaTestUtils {

    private static final VarHandle FIELD_MODIFIERS;

    static {
        try {
            FIELD_MODIFIERS = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup())
                    .findVarHandle(Field.class, "modifiers", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private LambdaTestUtils() {
    }

    /**
     * Callback for {@link #withFormattingSink(Path, FormattingSinkAction)}.
     */
    @FunctionalInterface
    public interface FormattingSinkAction {
        void run(FormattingLogSink sink) throws Exception;
    }

    /**
     * Opens a file-backed framed sink, runs {@code action}, then closes the stream so the file
     * is complete for {@link java.nio.file.Files#readAllBytes(Path)}.
     */
    public static void withFormattingSink(Path tempFile, FormattingSinkAction action) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
            FramedTelemetryLogSink delegate = new FramedTelemetryLogSink(fos.getFD());
            FormattingLogSink sink = new FormattingLogSink(delegate);
            action.run(sink);
        }
    }

    /**
     * Gets the current value of a static field on Lambda.Environment.
     */
    public static String getEnvironmentField(String fieldName) throws ReflectiveOperationException {
        Field f = Environment.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        String value = (String) f.get(null);
        return value;
    }

    /**
     * Sets a static final String field on Lambda.Environment.
     *
     * @return the previous value (for manual restore)
     */
    public static String setEnvironmentField(String fieldName, String value) throws ReflectiveOperationException {
        Field f = Environment.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        FIELD_MODIFIERS.set(f, f.getModifiers() & ~Modifier.FINAL);
        String previous = (String) f.get(null);
        f.set(null, value);
        return previous;
    }

    /**
     * Executes a block with the given Environment field set, then restores the previous value.
     */
    public static AutoCloseable withEnvironment(String fieldName, String value) throws ReflectiveOperationException {
        String previous = setEnvironmentField(fieldName, value);
        return () -> setEnvironmentField(fieldName, previous);
    }

    /**
     * Sets AWS_LAMBDA_INITIALIZATION_TYPE.
     *
     * @return the previous value
     */
    public static String setInitializationType(String value) throws ReflectiveOperationException {
        return setEnvironmentField("AWS_LAMBDA_INITIALIZATION_TYPE", value);
    }
}
