package info.pithos.runtime.core.util;

import info.pithos.runtime.model.protocol.Context.RequestContext;

public class Util {
  public static String createKey(RequestContext requestContext, String key) {
    checkRequestContext(requestContext);
    checkKey(key);
    return requestContext.getEnterpriseId() + ":" + key;
  }

  private static void checkKey(String key) {
    if (key == null || key.isBlank()) throw new IllegalArgumentException("key = null or blank");
  }

  private static void checkRequestContext(RequestContext requestContext) {
    if (requestContext == null) throw new IllegalArgumentException("requestContext == null");
  }

}
