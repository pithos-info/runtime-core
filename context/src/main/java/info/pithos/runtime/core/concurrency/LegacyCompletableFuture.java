package info.pithos.runtime.core.concurrency;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.pithos.runtime.core.context.SystemContext;

/**
 * @author svarma
 *
 *         June 8, 2021
 *
 */
public class LegacyCompletableFuture<V> extends CompletableFuture<V> {

	private static final Logger logger = LoggerFactory.getLogger(LegacyCompletableFuture.class);
	private final Future<V> future;
	private final SystemContext context;

	/**
	 * @param future
	 */
	public LegacyCompletableFuture(SystemContext context, Future<V> future) {
		if (context == null) {
			throw new IllegalArgumentException("null context");
		}

		if (future == null) {
			throw new IllegalArgumentException("null future");
		}

		this.context = context;
		this.future = future;
		this.context.getScheduledExecutor().schedule(this::process, 10, TimeUnit.MILLISECONDS);
	}

	private void process() {
		if (future.isDone()) {
			logger.debug("future is done");
			try {
				complete(future.get());
			} catch (InterruptedException e) {
				e.printStackTrace();
				completeExceptionally(e);
				logger.error("future to completableFuture conversion error", e);
			} catch (ExecutionException e) {
				e.printStackTrace();
				logger.error("future to completableFuture conversion erro", e);
				completeExceptionally(e.getCause());
			}
			return;
		}

		if (future.isCancelled()) {
			logger.debug("future is cancelled");
			cancel(true);
			return;
		}

		context.getScheduledExecutor().schedule(this::process, 10, TimeUnit.MILLISECONDS);
	}
}
