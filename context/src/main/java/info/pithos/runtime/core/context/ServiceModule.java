package info.pithos.runtime.core.context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.inject.AbstractModule;

/**
 * @author svarma
 *
 *         June 6, 2021
 *
 */
public abstract class ServiceModule extends AbstractModule {

	private final ApplicationContext context;
	protected final AtomicBoolean initialized;

	/**
	 * @param context
	 */
	public ServiceModule(ApplicationContext context) {
		if (context == null) {
			throw new IllegalArgumentException("null context");
		}

		this.context = context;
		this.initialized = new AtomicBoolean();
	}

	protected abstract boolean init();

	/**
	 * Opens connections for all {@link ServiceLifeCycle} clients owned by this module.
	 * Called by {@link ApplicationContext#start} after all modules have been init-ed.
	 * Modules with no infrastructure clients must return {@code CompletableFuture.completedFuture(true)}.
	 */
	public abstract CompletableFuture<Boolean> start(long timeout, TimeUnit unit);

	/**
	 * Closes connections in the reverse order they were opened in {@link #start}.
	 * Called by {@link ApplicationContext#shutdown} before executor pools are torn down.
	 * Modules with no infrastructure clients must return {@code CompletableFuture.completedFuture(true)}.
	 */
	public abstract CompletableFuture<Boolean> shutdown(long timeout, TimeUnit unit);

	protected ApplicationContext getApplicationContext() {
		return this.context;
	}
}
