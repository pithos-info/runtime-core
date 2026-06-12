package info.pithos.runtime.core.util;

import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;

public class Util {

  public static String createKey(RequestContext requestContext, String key) {
    checkRequestContext(requestContext);
    checkKey(key);
    return requestContext.getEnterpriseId() + ":" + key;
  }

  /** Formats a list of strings as a Postgres array literal, e.g. {@code {"a","b"}}. */
  public static String pgArray(List<String> values) {
    if (values.isEmpty()) return "{}";
    StringBuilder sb = new StringBuilder("{");
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) sb.append(',');
      sb.append('"');
      sb.append(values.get(i).replace("\\", "\\\\").replace("\"", "\\\""));
      sb.append('"');
    }
    return sb.append('}').toString();
  }

  private static void checkKey(String key) {
    if (key == null || key.isBlank()) throw new IllegalArgumentException("key = null or blank");
  }

  private static void checkRequestContext(RequestContext requestContext) {
    if (requestContext == null) throw new IllegalArgumentException("requestContext == null");
  }
}
