package info.pithos.runtime.core.context;

import com.google.inject.Injector;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface ApplicationContext {

	SystemContext getSystemContext();

	Injector getInjector();

	/**
	 * Starts all registered service modules in parallel.
	 * Each module is responsible for starting the clients it owns.
	 * Blocks-free — callers decide whether to join or compose.
	 */
	CompletableFuture<Void> start(long timeout, TimeUnit unit);

	/**
	 * Shuts down all registered service modules in parallel, then tears down
	 * the system-context executor pools.
	 */
	CompletableFuture<Void> shutdown(long timeout, TimeUnit unit);
}
