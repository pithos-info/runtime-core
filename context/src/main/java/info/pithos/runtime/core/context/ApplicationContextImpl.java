package info.pithos.runtime.core.context;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * @author svarma
 *
 *         June 6, 2021
 *
 */
public class ApplicationContextImpl implements ApplicationContext {
	private final SystemContext systemContext;
	private final ContextCreator creator;
	private final Injector injector;

	/**
	 * @param creator
	 */
	public ApplicationContextImpl(ContextCreator creator) {
		this.systemContext = new SystemContextImpl(creator);
		((SystemContextImpl) this.systemContext).wireApplicationContext(this);

		this.creator = creator;
		Iterable<ServiceModule> serviceModules = this.creator.getInjectionModules(this);
		this.initInjectionModules(serviceModules);
		this.injector = Guice.createInjector(serviceModules);
	}

	@Override
	public Injector getInjector() {
		return this.injector;
	}

	/**
	 * @param moduleClasses
	 */
	private Iterable<ServiceModule> initInjectionModules(Iterable<ServiceModule> serviceModules) {
		serviceModules.forEach((module) -> {
			// CompletableFuture.runAsync(() -> {
			module.init();
			// });
		});

		return serviceModules;
	}

	@Override
	public SystemContext getSystemContext() {
		return this.systemContext;
	}
}
