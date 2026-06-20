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

import com.google.inject.Guice;
import com.google.inject.Injector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
	private final List<ServiceModule> modules;

	public ApplicationContextImpl(ContextCreator creator) {
		this.systemContext = new SystemContextImpl(creator);
		((SystemContextImpl) this.systemContext).wireApplicationContext(this);

		this.creator = creator;

		List<ServiceModule> moduleList = new ArrayList<>();
		this.creator.getInjectionModules(this).forEach(moduleList::add);
		this.modules = Collections.unmodifiableList(moduleList);

		this.modules.forEach(ServiceModule::init);
		this.injector = Guice.createInjector(this.modules);
	}

	@Override
	public Injector getInjector() {
		return this.injector;
	}

	@Override
	public SystemContext getSystemContext() {
		return this.systemContext;
	}

	@Override
	public CompletableFuture<Void> start(long timeout, TimeUnit unit) {
		CompletableFuture<?>[] futures = modules.stream()
			.map(m -> m.start(timeout, unit))
			.toArray(CompletableFuture[]::new);
		return CompletableFuture.allOf(futures);
	}

	@Override
	public CompletableFuture<Void> shutdown(long timeout, TimeUnit unit) {
		CompletableFuture<?>[] futures = modules.stream()
			.map(m -> m.shutdown(timeout, unit))
			.toArray(CompletableFuture[]::new);
		return CompletableFuture.allOf(futures)
			.whenComplete((v, ex) -> systemContext.shutdown(unit.toMillis(timeout)));
	}
}
