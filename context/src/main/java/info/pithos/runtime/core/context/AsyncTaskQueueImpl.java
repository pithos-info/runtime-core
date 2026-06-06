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
