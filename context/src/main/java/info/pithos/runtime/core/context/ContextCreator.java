package info.pithos.runtime.core.context;

import info.pithos.runtime.model.config.Config.ConfigMap;

/**
 * @author svarma
 *
 * June 6, 2021
 *
 */
public interface ContextCreator {
  
  /**
   * @return
   */
  ConfigMap getConfigMap();

  /**
   * @param context
   * @return
   */
  Iterable<ServiceModule> getInjectionModules(ApplicationContext context);  
}
