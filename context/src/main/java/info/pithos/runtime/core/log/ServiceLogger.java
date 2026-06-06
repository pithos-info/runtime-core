package info.pithos.runtime.core.log;

import org.slf4j.Logger;
import info.pithos.runtime.model.protocol.http.Context.LogLevelType;
import info.pithos.runtime.model.protocol.http.Context.RequestContext;

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
