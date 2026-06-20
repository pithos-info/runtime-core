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

import java.util.logging.Level;

import org.slf4j.Logger;
import info.pithos.runtime.model.protocol.Context.LogLevelType;
import info.pithos.runtime.model.protocol.Context.RequestContext;

/**
 * @author svarma
 *
 */
public class ServiceLoggerImpl implements ServiceLogger {

  @Override
  public void logRequest(RequestContext requestContext, Logger logger, LogLevelType loglevel, String message,
      Object... args) {
    this.log(requestContext, logger, loglevel, null, null, message, args);
  }

  @Override
  public void logRequest(RequestContext requestContext, Logger logger, LogLevelType loglevel, Throwable throwable,
      String message, Object... args) {

    this.log(requestContext, logger, loglevel, null, throwable, message, args);
  }

  @Override
  public void logRequest(RequestContext requestContext, Logger logger, LogLevelType loglevel, Exception exception,
      String message, Object... args) {
    this.log(requestContext, logger, loglevel, exception, null, message, args);
  }

  /**
   * @param requestContext
   * @param logger
   * @param loglevel
   * @param exception
   * @param throwable
   * @param message
   * @param args
   */
  private void log(RequestContext requestContext, Logger logger, LogLevelType loglevel, Exception exception,
      Throwable throwable, String message, Object... args) {

    if (logger == null) {
      throw new IllegalArgumentException("null logger");
    }

    LogLevelType useLoglevel = requestContext != null ? findLogLevel(logger, requestContext.getLogLevel(), loglevel)
        : loglevel;

    String logMessage;
    if (requestContext != null) {
      StringBuilder sb = new StringBuilder();
      sb.append("requestId:[");
      sb.append(requestContext.getRequestId());
      sb.append("], ");

      if (requestContext.hasAuthContext()) {
        sb.append("enterpriseId:[");
        sb.append(requestContext.getAuthContext().getEnterpriseId());
        sb.append("], ");

        sb.append("userId:[");
        sb.append(requestContext.getAuthContext().getUserId());
        sb.append("], ");
      }

      sb.append(message);
      logMessage = sb.toString();
    } else {
      logMessage = message;
    }

    switch (useLoglevel) {
    case ERROR:
      if (exception != null) {
        logger.error(logMessage, exception, args);
      } else if (throwable != null) {
        logger.error(logMessage, throwable, args);
      } else {
        logger.error(logMessage, args);
      }
      break;

    case WARN:
      if (exception != null) {
        logger.warn(logMessage, exception, args);
      } else if (throwable != null) {
        logger.warn(logMessage, throwable, args);
      } else {
        logger.warn(logMessage, args);
      }
      break;

    case INFO:
      if (exception != null) {
        logger.info(logMessage, exception, args);
      } else if (throwable != null) {
        logger.info(logMessage, throwable, args);
      } else {
        logger.info(logMessage, args);
      }
      break;

    case DEBUG:
      if (exception != null) {
        logger.debug(logMessage, exception, args);
      } else if (throwable != null) {
        logger.debug(logMessage, throwable, args);
      } else {
        logger.debug(logMessage, args);
      }
      break;

    case TRACE:
      if (exception != null) {
        logger.trace(logMessage, exception, args);
      } else if (throwable != null) {
        logger.trace(logMessage, throwable, args);
      } else {
        logger.trace(logMessage, args);
      }
    } // switch
  }

  /**
   * @param logger
   * @param requestLoglevel
   * @param log
   * @return
   */
  private LogLevelType findLogLevel(Logger logger, LogLevelType requestLoglevel, LogLevelType loglevel) {
    LogLevelType use = loglevel;

    if (requestLoglevel != null) {
      use = requestLoglevel;
      switch (requestLoglevel) {
      case INFO:
        if (!logger.isInfoEnabled()) {
          use = LogLevelType.ERROR;
        }

        break;

      case DEBUG:
        if (!logger.isDebugEnabled()) {
          if (!logger.isInfoEnabled()) {
            use = LogLevelType.ERROR;
          } else {
            use = LogLevelType.INFO;
          }
        }

        break;
      case TRACE:
        if (!logger.isTraceEnabled()) {
          if (!logger.isDebugEnabled()) {
            if (!logger.isInfoEnabled()) {
              use = LogLevelType.ERROR;
            } else {
              use = LogLevelType.INFO;
            }
          } else {
            use = LogLevelType.DEBUG;
          }
        }

        break;

      } // switch
    }

    return use;
  }
}
