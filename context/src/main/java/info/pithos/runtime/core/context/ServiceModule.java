package info.pithos.runtime.core.context;

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

	/**
	 * @return
	 */
	protected abstract boolean init();

	/**
	 * @return
	 */
	protected ApplicationContext getApplicationContext() {
		return this.context;
	}
}
