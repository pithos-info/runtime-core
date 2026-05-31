package info.pithos.runtime.core.context;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import info.pithos.runtime.core.log.ServiceLogger;
import info.pithos.runtime.core.log.ServiceLoggerImpl;
import info.pithos.runtime.model.config.Config.ConfigMap;

/**
 * @author svarma
 *
 *         June 6, 2021
 *
 */
public class SystemContextImpl implements SystemContext {
	private final String serviceName;
	private final ConfigMap configMap;
	private final ServiceConfigs serviceConfigs;
	private final ForkJoinPool forkJoinExecutor;
	private final ScheduledThreadPoolExecutor scheduledExecutor;
	private final ContextCreator creator;
	private final ServiceLogger logger;

	/**
	 * @param creator
	 */
	protected SystemContextImpl(ContextCreator creator) {
		if (creator == null) {
			throw new IllegalArgumentException("null creator");
		}

		if (creator.getConfigMap() == null) {
			throw new IllegalArgumentException("null configMap");
		}

		if (creator.getConfigMap().getBootstrapConfigs().getServiceName() == null
		    || creator.getConfigMap().getBootstrapConfigs().getServiceName().isEmpty()) {
			throw new IllegalArgumentException("null or empty serviceName");
		}

		this.creator = creator;
		this.configMap = this.creator.getConfigMap();

		this.serviceConfigs = new ServiceConfigs(this.configMap);

		this.serviceName = this.configMap.getBootstrapConfigs().getServiceName();
		int multiplier = this.configMap.getBootstrapConfigs().getMultiplier();
		int poolSize = Runtime.getRuntime().availableProcessors() * multiplier;
		this.forkJoinExecutor = new ForkJoinPool(poolSize);
		this.scheduledExecutor = new ScheduledThreadPoolExecutor(poolSize);

		this.logger = new ServiceLoggerImpl();
	}

	@Override
	public String getServiceName() {
		return serviceName;
	}

	@Override
	public ForkJoinPool getForkJoinExecutor() {
		return forkJoinExecutor;
	}

	@Override
	public ScheduledThreadPoolExecutor getScheduledExecutor() {
		return scheduledExecutor;
	}

	@Override
	public ServiceLogger getLogger() {
		return this.logger;
	}

	@Override
	public boolean shutdown(long ms) {
		try {
			this.forkJoinExecutor.awaitTermination(ms, TimeUnit.MILLISECONDS);
			this.scheduledExecutor.awaitTermination(ms, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	@Override
	public ConfigMap getConfigMap() {
		return this.configMap;
	}

	@Override
	public ServiceConfigs getServiceConfigs() {
		return this.serviceConfigs;
	}
}
