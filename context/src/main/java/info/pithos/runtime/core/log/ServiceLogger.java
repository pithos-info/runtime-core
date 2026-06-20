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

import org.slf4j.Logger;
import info.pithos.runtime.model.protocol.Context.LogLevelType;
import info.pithos.runtime.model.protocol.Context.RequestContext;

/**
 * @author svarma
 *
 */
public interface ServiceLogger {

  /**
   * Log with requestId, enterpriseId, userId and at the log level set in request
   * context.
   * 
   * 
   * @param requestContext
   * @param logger
   * @param loglevel
   * @param message
   * @param args
   */
  void logRequest(RequestContext requestContext, Logger logger, LogLevelType loglevel, String message, Object... args);

  /**
   * Log with requestId, enterpriseId, userId and at the log level set in request
   * context.
   * 
   * 
   * @param requestContext
   * @param logger
   * @param loglevel
   * @param throwable
   * @param message
   * @param args
   */
  void logRequest(RequestContext requestContext, Logger logger, LogLevelType loglevel, Throwable throwable,
      String message, Object... args);

  /**
   * Log with requestId, enterpriseId, userId and at the log level set in request
   * context.
   * 
   * 
   * @param requestContext
   * @param logger
   * @param loglevel
   * @param exception
   * @param message
   * @param args
   */
  void logRequest(RequestContext requestContext, Logger logger, LogLevelType loglevel, Exception exception,
      String message, Object... args);
}
