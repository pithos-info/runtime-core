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

package info.pithos.runtime.core.context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Package-private implementation of {@link AsyncTaskQueue}. The underlying
 * {@link ThreadPoolExecutor} is owned and created by {@link SystemContextImpl};
 * this class wraps it and holds a reference to {@link ApplicationContext} for
 * future logging and metrics instrumentation.
 *
 * <p>{@link ApplicationContext} is unavailable at construction time (it is created
 * after {@link SystemContext}) and is injected via
 * {@link #setApplicationContext(ApplicationContext)} called from
 * {@link ApplicationContextImpl} immediately after it is fully constructed.
 */
class AsyncTaskQueueImpl implements AsyncTaskQueue {

    private final ThreadPoolExecutor executor;
    private volatile ApplicationContext applicationContext;

    AsyncTaskQueueImpl(ThreadPoolExecutor executor) {
        if (executor == null) throw new IllegalArgumentException("executor must not be null");
        this.executor = executor;
    }

    /** Called by {@link ApplicationContextImpl} once the context is fully wired. */
    void setApplicationContext(ApplicationContext ctx) {
        this.applicationContext = ctx;
    }

    ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void enqueue(Runnable task) {
        executor.submit(task);
    }

    /** No-op — the executor is created and started by {@link SystemContextImpl}. */
    @Override
    public CompletableFuture<Boolean> start(long timeout, TimeUnit unit) {
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Graceful shutdown: stops accepting new tasks, drains the queue, and waits
     * up to {@code timeout} for in-flight tasks to finish.
     */
    @Override
    public CompletableFuture<Boolean> shutdown(long timeout, TimeUnit unit) {
        return CompletableFuture.supplyAsync(() -> {
            executor.shutdown();
            try {
                return executor.awaitTermination(timeout, unit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        });
    }

    /**
     * Hard shutdown: interrupts running tasks, drops all pending tasks, and waits
     * up to {@code timeout} for worker threads to exit.
     */
    @Override
    public CompletableFuture<Boolean> forceShutdown(long timeout, TimeUnit unit) {
        return CompletableFuture.supplyAsync(() -> {
            executor.shutdownNow();
            try {
                return executor.awaitTermination(timeout, unit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        });
    }
}
