package com.smirnoal.lambda;

import com.smirnoal.lambda.Lambda.Constants;
import com.smirnoal.lambda.rapid.client.LambdaError;
import com.smirnoal.lambda.rapid.client.LambdaRapidHttpClient;
import com.smirnoal.lambda.rapid.client.dto.InvocationRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LambdaApplicationSnapStartTest {

    private static final class TestCompleteException extends RuntimeException {
        TestCompleteException() {
            super("test complete");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        SnapStartTestHooks.reset();
    }

    @Test
    void snapStart_runsBeforeHooksInRegistrationOrder_thenRestoreNext_thenAfterHooksInRegistrationOrder() throws Exception {
        List<String> order = new ArrayList<>();
        SnapStartTestHooks.registerBeforeSnapshot(() -> order.add("before-1"));
        SnapStartTestHooks.registerBeforeSnapshot(() -> order.add("before-2"));
        SnapStartTestHooks.registerAfterRestore(() -> order.add("after-1"));
        SnapStartTestHooks.registerAfterRestore(() -> order.add("after-2"));

        SnapStartTestHooks.setInitializationType(Constants.INITIALIZATION_TYPE_SNAP_START);

        AtomicInteger restoreNextCalls = new AtomicInteger(0);

        LambdaRapidHttpClient mock = new MockClient(
                restoreNextCalls::incrementAndGet,
                null,
                null);

        runApplicationExpectingTestComplete(mock);

        assertEquals(1, restoreNextCalls.get());
        assertEquals(List.of("before-1", "before-2", "after-1", "after-2"), order);
    }

    @Test
    void nonSnapStart_skipsLifecycle() throws Exception {
        SnapStartTestHooks.registerBeforeSnapshot(() -> {
            throw new AssertionError("should not run");
        });

        SnapStartTestHooks.setInitializationType("on-demand");

        AtomicInteger restoreNextCalls = new AtomicInteger(0);
        LambdaRapidHttpClient mock = new MockClient(
                restoreNextCalls::incrementAndGet,
                null,
                null);

        runApplicationExpectingTestComplete(mock);

        assertEquals(0, restoreNextCalls.get());
    }

    @Test
    void beforeSnapshotHookFailure_reportsInitErrorWithBeforeSnapshotErrorType() throws Exception {
        SnapStartTestHooks.registerBeforeSnapshot(() -> {
            throw new RuntimeException("before hook failed");
        });

        SnapStartTestHooks.setInitializationType(Constants.INITIALIZATION_TYPE_SNAP_START);

        AtomicReference<LambdaError> initError = new AtomicReference<>();
        LambdaRapidHttpClient mock = new MockClient(null, initError::set, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                MockSnapStartLambdaRapidHttpClientProvider.withClient(mock,
                        () -> new LambdaApplication().run(() -> {})));
        assertInstanceOf(RuntimeException.class, ex.getCause());
        assertEquals("before hook failed", ex.getCause().getMessage());

        assertNotNull(initError.get());
        assertEquals(Constants.ERROR_TYPE_BEFORE_SNAPSHOT, initError.get().errorRequest().errorType());
    }

    @Test
    void afterRestoreHookFailure_reportsRestoreErrorWithAfterRestoreErrorType() throws Exception {
        SnapStartTestHooks.registerAfterRestore(() -> {
            throw new RuntimeException("after hook failed");
        });

        SnapStartTestHooks.setInitializationType(Constants.INITIALIZATION_TYPE_SNAP_START);

        AtomicReference<LambdaError> restoreError = new AtomicReference<>();
        LambdaRapidHttpClient mock = new MockClient(null, null, restoreError::set);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                MockSnapStartLambdaRapidHttpClientProvider.withClient(mock,
                        () -> new LambdaApplication().run(() -> {})));
        assertInstanceOf(RuntimeException.class, ex.getCause());
        assertEquals("after hook failed", ex.getCause().getMessage());

        assertNotNull(restoreError.get());
        assertEquals(Constants.ERROR_TYPE_AFTER_RESTORE, restoreError.get().errorRequest().errorType());
    }

    private static void runApplicationExpectingTestComplete(LambdaRapidHttpClient mock) {
        MockSnapStartLambdaRapidHttpClientProvider.withClient(mock, () ->
                assertThrows(TestCompleteException.class, () -> new LambdaApplication().run(() -> {})));
    }

    private static final class MockClient implements LambdaRapidHttpClient {
        private final Runnable onRestoreNext;
        private final Consumer<LambdaError> onInitError;
        private final Consumer<LambdaError> onReportRestoreError;

        MockClient(Runnable onRestoreNext,
                   Consumer<LambdaError> onInitError,
                   Consumer<LambdaError> onReportRestoreError) {
            this.onRestoreNext = onRestoreNext;
            this.onInitError = onInitError;
            this.onReportRestoreError = onReportRestoreError;
        }

        @Override
        public void initError(LambdaError error) {
            if (onInitError != null) {
                onInitError.accept(error);
            }
        }

        @Override
        public InvocationRequest next() {
            throw new TestCompleteException();
        }

        @Override
        public void reportInvocationSuccess(String requestId, byte[] response) {
        }

        @Override
        public void reportInvocationError(String requestId, LambdaError error) {
        }

        @Override
        public void restoreNext() {
            if (onRestoreNext != null) {
                onRestoreNext.run();
            }
        }

        @Override
        public void reportRestoreError(LambdaError error) {
            if (onReportRestoreError != null) {
                onReportRestoreError.accept(error);
            }
        }
    }
}
