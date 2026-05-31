package info.pithos.runtime.core.context;

import com.google.inject.Injector;

/**
 * @author svarma
 *
 * June 6, 2021
 *
 */
public interface ApplicationContext {

	/**
	 * @return
	 */
	SystemContext getSystemContext();
	
	/**
	 * @return
	 */
	Injector getInjector();
}
