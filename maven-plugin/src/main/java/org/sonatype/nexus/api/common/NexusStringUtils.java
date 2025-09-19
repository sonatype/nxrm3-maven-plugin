/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.nexus.api.common;

/**
 * Simple string operations in lieu of adding additional libraries that provide these
 *
 * @since 3.0
 */
public class NexusStringUtils
{
  private NexusStringUtils() {
  }

  public static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  public static boolean isNotBlank(String s) {
    return !isBlank(s);
  }
}
