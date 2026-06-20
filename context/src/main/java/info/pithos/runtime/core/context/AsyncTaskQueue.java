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
import java.util.concurrent.TimeUnit;

/**
 * Background task queue for fire-and-forget work that must not block the request path.
 * Implements {@link ServiceLifeCycle} so it participates in the same managed lifecycle
 * as other platform components.
 *
 * <p>When the internal queue is at capacity, incoming tasks are silently dropped —
 * callers must not rely on every task completing (appropriate for cache write-backs,
 * which are best-effort by design).
 *
 * <p>Two shutdown modes:
 * <ul>
 *   <li>{@link #shutdown} — graceful: stops accepting new tasks, drains the queue,
 *       waits up to the timeout for in-flight tasks to complete.</li>
 *   <li>{@link #forceShutdown} — hard: interrupts running tasks, drops all pending
 *       ones, then waits up to the timeout for worker threads to exit.</li>
 * </ul>
 */
public interface AsyncTaskQueue extends ServiceLifeCycle {

    /**
     * Enqueues {@code task} for background execution. Returns immediately.
     * If the internal queue is full the task is dropped silently.
     */
    void enqueue(Runnable task);

    /**
     * Hard shutdown: interrupts running tasks, drops all pending tasks, then waits
     * up to {@code timeout} for the worker threads to exit.
     * Returns {@code true} if all threads terminated within the timeout.
     */
    CompletableFuture<Boolean> forceShutdown(long timeout, TimeUnit unit);
}
