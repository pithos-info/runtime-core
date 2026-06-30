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

import com.google.inject.Injector;
import info.pithos.runtime.core.metrics.MetricsCommitter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface ApplicationContext {

	SystemContext getSystemContext();

	Injector getInjector();

	MetricsCommitter getMetricsCommitter();

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
