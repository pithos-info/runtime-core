package info.pithos.runtime.core.concurrency;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;

import info.pithos.runtime.core.context.ServiceConfigs;
import info.pithos.runtime.core.context.SystemContext;
import info.pithos.runtime.core.log.ServiceLogger;
import info.pithos.runtime.model.config.Config.ConfigMap;
import info.pithos.runtime.model.protocol.http.RequestContextOuterClass.LogLevelType;

import java.util.concurrent.ForkJoinPool;

class LegacyCompletableFutureTest {

    private ScheduledThreadPoolExecutor executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private SystemContext contextWith(ScheduledThreadPoolExecutor exec) {
        return new SystemContext() {
            @Override public String getServiceName() { return "test"; }
            @Override public ForkJoinPool getForkJoinExecutor() { return null; }
            @Override public ScheduledThreadPoolExecutor getScheduledExecutor() { return exec; }
            @Override public ServiceLogger getLogger() { return null; }
            @Override public boolean shutdown(long ms) { return true; }
            @Override public ConfigMap getConfigMap() { return null; }
            @Override public ServiceConfigs getServiceConfigs() { return null; }
        };
    }

    // --- constructor guards ---

    @Test
    void constructor_nullContext_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> new LegacyCompletableFuture<>(null, CompletableFuture.completedFuture("x")));
    }

    @Test
    void constructor_nullFuture_throws() {
        executor = new ScheduledThreadPoolExecutor(1);
        assertThrows(IllegalArgumentException.class,
            () -> new LegacyCompletableFuture<>(contextWith(executor), null));
    }

    // --- completion ---

    @Test
    void alreadyDoneFuture_completesWithResult() throws Exception {
        executor = new ScheduledThreadPoolExecutor(1);
        Future<String> done = CompletableFuture.completedFuture("hello");
        LegacyCompletableFuture<String> lcf = new LegacyCompletableFuture<>(contextWith(executor), done);
        assertEquals("hello", lcf.get(2, TimeUnit.SECONDS));
    }

    @Test
    void pendingFuture_completesAfterFutureDone() throws Exception {
        executor = new ScheduledThreadPoolExecutor(1);
        CompletableFuture<String> inner = new CompletableFuture<>();
        LegacyCompletableFuture<String> lcf = new LegacyCompletableFuture<>(contextWith(executor), inner);

        executor.schedule(() -> inner.complete("world"), 50, TimeUnit.MILLISECONDS);

        assertEquals("world", lcf.get(2, TimeUnit.SECONDS));
    }

    // --- exception propagation ---

    @Test
    void failedFuture_completesExceptionally() {
        executor = new ScheduledThreadPoolExecutor(1);
        CompletableFuture<String> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("boom"));

        LegacyCompletableFuture<String> lcf = new LegacyCompletableFuture<>(contextWith(executor), failed);

        ExecutionException ex = assertThrows(ExecutionException.class,
            () -> lcf.get(2, TimeUnit.SECONDS));
        assertEquals("boom", ex.getCause().getMessage());
    }

    @Test
    void pendingFuture_thenFails_completesExceptionally() throws Exception {
        executor = new ScheduledThreadPoolExecutor(1);
        CompletableFuture<Integer> inner = new CompletableFuture<>();
        LegacyCompletableFuture<Integer> lcf = new LegacyCompletableFuture<>(contextWith(executor), inner);

        executor.schedule(
            () -> inner.completeExceptionally(new IllegalStateException("oops")),
            50, TimeUnit.MILLISECONDS);

        ExecutionException ex = assertThrows(ExecutionException.class,
            () -> lcf.get(2, TimeUnit.SECONDS));
        assertInstanceOf(IllegalStateException.class, ex.getCause());
    }
}
