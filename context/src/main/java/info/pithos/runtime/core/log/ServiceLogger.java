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

package info.pithos.runtime.core.log;

import info.pithos.runtime.model.protocol.Context.LogLevelType;
import info.pithos.runtime.model.protocol.Context.RequestContext;

/**
 * @author svarma
 *
 */
public interface ServiceLogger {

  void logRequest(RequestContext requestContext, Class<?> clazz, LogLevelType loglevel, String message, Object... args);

  void logRequest(RequestContext requestContext, Class<?> clazz, LogLevelType loglevel, Throwable throwable,
      String message, Object... args);

  void logRequest(RequestContext requestContext, Class<?> clazz, LogLevelType loglevel, Exception exception,
      String message, Object... args);
}
