package com.smirnoal.crema;

import com.smirnoal.crema.Lambda.Constants;
import com.smirnoal.crema.Lambda.SnapStart;
import com.smirnoal.crema.rapid.client.LambdaError;
import com.smirnoal.crema.rapid.client.LambdaRapidHttpClient;
import com.smirnoal.crema.rapid.client.dto.InvocationRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

class SnapStartLambdaApplicationTest {

    private final Runnable handler = () -> {};
    private final Runnable runLambdaHandler = () -> new LambdaApplication().run(handler);

    private static final class TestCompleteException extends RuntimeException {
        TestCompleteException() {
            super("test complete");
        }
    }

    @BeforeEach
    void setup() throws Exception {
        LambdaTestUtils.setInitializationType(Constants.INITIALIZATION_TYPE_SNAP_START);
    }

    @AfterEach
    void tearDown() throws Exception {
        SnapStartTestUtils.reset();
    }

    @Test
    void snapStart_runsBeforeHooksInRegistrationOrder_thenRestoreNext_thenAfterHooksInRegistrationOrder() throws Exception {
        List<String> order = new ArrayList<>();
        SnapStart.registerBeforeSnapshotHook(() -> order.add("before-1"));
        SnapStart.registerBeforeSnapshotHook(() -> order.add("before-2"));
        SnapStart.registerAfterRestoreHook(() -> order.add("after-1"));
        SnapStart.registerAfterRestoreHook(() -> order.add("after-2"));

        AtomicInteger restoreNextCalls = new AtomicInteger(0);

        LambdaRapidHttpClient mock = new MockClient(
                restoreNextCalls::incrementAndGet,
                null,
                null);

        SnapStartMockLambdaRapidHttpClientProvider.withClient(mock, () ->
                assertThrows(TestCompleteException.class, runLambdaHandler::run)
        );

        assertEquals(1, restoreNextCalls.get());
        assertEquals(List.of("before-1", "before-2", "after-1", "after-2"), order);
    }

    @Test
    void nonSnapStart_skipsLifecycle() throws Exception {
        SnapStart.registerBeforeSnapshotHook(() -> {
            throw new AssertionError("before snapshot hook should not run");
        });
        SnapStart.registerAfterRestoreHook(() -> {
            throw new AssertionError("after restore hook should not run");
        });

        LambdaTestUtils.setInitializationType("on-demand");

        AtomicInteger restoreNextCalls = new AtomicInteger(0);
        LambdaRapidHttpClient mock = new MockClient(
                restoreNextCalls::incrementAndGet,
                null,
                null);

        SnapStartMockLambdaRapidHttpClientProvider.withClient(mock, () ->
                assertThrows(TestCompleteException.class, runLambdaHandler::run)
        );

        assertEquals(0, restoreNextCalls.get());
    }

    @Test
    void beforeSnapshotHookFailure_reportsInitErrorWithBeforeSnapshotErrorType() throws Exception {
        SnapStart.registerBeforeSnapshotHook(() -> {
            throw new RuntimeException("before hook failed");
        });

        AtomicReference<LambdaError> initError = new AtomicReference<>();
        LambdaRapidHttpClient mock = new MockClient(null, initError::set, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                SnapStartMockLambdaRapidHttpClientProvider.withClient(mock, runLambdaHandler));
        assertInstanceOf(RuntimeException.class, ex.getCause());
        assertEquals("before hook failed", ex.getCause().getMessage());

        assertNotNull(initError.get());
        assertEquals(Constants.ERROR_TYPE_BEFORE_SNAPSHOT, initError.get().errorRequest().errorType());
    }

    @Test
    void afterRestoreHookFailure_reportsRestoreErrorWithAfterRestoreErrorType() throws Exception {
        SnapStart.registerAfterRestoreHook(() -> {
            throw new RuntimeException("after hook failed");
        });

        AtomicReference<LambdaError> restoreError = new AtomicReference<>();
        LambdaRapidHttpClient mock = new MockClient(null, null, restoreError::set);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                SnapStartMockLambdaRapidHttpClientProvider.withClient(mock, runLambdaHandler));
        assertInstanceOf(RuntimeException.class, ex.getCause());
        assertEquals("after hook failed", ex.getCause().getMessage());

        assertNotNull(restoreError.get());
        assertEquals(Constants.ERROR_TYPE_AFTER_RESTORE, restoreError.get().errorRequest().errorType());
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
