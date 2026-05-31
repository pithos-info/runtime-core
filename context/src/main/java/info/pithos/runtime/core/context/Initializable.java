package info.pithos.runtime.core.context;

import java.util.concurrent.CompletableFuture;

/**
 * @author svarma
 *
 * June 6, 2021
 *
 */
public interface Initializable {

	/**
	 * @return
	 */
	CompletableFuture<Boolean> init();
	
}
