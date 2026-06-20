/*
 * Copyright 2026 Pithos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package info.pithos.runtime.core.concurrency;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;

import info.pithos.runtime.core.context.AsyncTaskQueue;
import info.pithos.runtime.core.context.ServiceConfigs;
import info.pithos.runtime.core.context.SystemContext;
import info.pithos.runtime.core.log.ServiceLogger;
import info.pithos.runtime.model.config.Config.ConfigMap;
import info.pithos.runtime.model.protocol.Context.LogLevelType;

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
            @Override public AsyncTaskQueue getTaskQueue() { return null; }
            @Override public <T> CompletableFuture<T> submitAsync(Callable<T> task) { return null;}
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
