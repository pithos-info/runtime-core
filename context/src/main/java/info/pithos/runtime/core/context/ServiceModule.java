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
