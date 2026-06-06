package info.pithos.runtime.core.context;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.google.inject.Injector;
import info.pithos.runtime.core.log.ServiceLogger;
import info.pithos.runtime.model.config.Config.ConfigMap;

/**
 * @author svarma
 *
 * June 6, 2021
 *
 */
public interface SystemContext {
	/**
	 * @return
	 */
	String getServiceName();

	/**
	 * @return
	 */
	ForkJoinPool getForkJoinExecutor();

	/**
	 * @return
	 */
	ScheduledThreadPoolExecutor getScheduledExecutor();

	/**
	 * @return
	 */
	ServiceLogger getLogger();

	/**
	 * @param ms
	 * @return
	 */
	boolean shutdown(long ms);
	
	/**
	 * @return
	 */
	ConfigMap getConfigMap();

	/**
	 * @return
	 */
	ServiceConfigs getServiceConfigs();

	/**
	 * @return
	 */
	AsyncTaskQueue getTaskQueue();
}
