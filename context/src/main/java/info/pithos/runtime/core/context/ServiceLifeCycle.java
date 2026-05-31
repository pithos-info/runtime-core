package info.pithos.runtime.core.context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author svarma
 *
 *         June 6, 2021
 *
 */
public interface ServiceLifeCycle {

	/**
	 * @return
	 */
	CompletableFuture<Boolean> start(long timeout, TimeUnit unit);

	/**
	 * @return
	 */
	CompletableFuture<Boolean> shutdown(long timeout, TimeUnit unit);
}
