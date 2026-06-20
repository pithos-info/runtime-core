/*
 * Copyright 2026 Pithos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package info.pithos.runtime.core.context;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
	private static final int TASK_QUEUE_CAPACITY = 10_000;

	private final String serviceName;
	private final ConfigMap configMap;
	private final ServiceConfigs serviceConfigs;
	private final ForkJoinPool forkJoinExecutor;
	private final ScheduledThreadPoolExecutor scheduledExecutor;
	private final ThreadPoolExecutor taskExecutor;
	private final AsyncTaskQueueImpl taskQueue;
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

		int taskThreads = Math.max(2, poolSize / 2);
		AtomicInteger taskIdx = new AtomicInteger();
		this.taskExecutor = new ThreadPoolExecutor(
				taskThreads, taskThreads,
				60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(TASK_QUEUE_CAPACITY),
				r -> {
					Thread t = new Thread(r, "bg-task-" + taskIdx.getAndIncrement());
					t.setDaemon(true);
					return t;
				},
				new ThreadPoolExecutor.DiscardPolicy()
		);
		this.taskQueue = new AsyncTaskQueueImpl(this.taskExecutor);

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
			this.taskQueue.shutdown(ms, TimeUnit.MILLISECONDS).join();
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

	@Override
	public AsyncTaskQueue getTaskQueue() {
		return this.taskQueue;
	}
	@Override
	public <T> CompletableFuture<T> submitAsync(Callable<T> task) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return task.call();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, this.getForkJoinExecutor());
	}


	/** Called by {@link ApplicationContextImpl} immediately after it is fully constructed. */
	void wireApplicationContext(ApplicationContext ctx) {
		this.taskQueue.setApplicationContext(ctx);
	}
}
