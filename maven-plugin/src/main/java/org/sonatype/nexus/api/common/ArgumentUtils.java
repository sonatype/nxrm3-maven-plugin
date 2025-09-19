/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.nexus.api.common;

import static org.sonatype.nexus.api.common.NexusStringUtils.isNotBlank;

/**
 * Simple helper class for argument checks
 *
 * @since 3.0
 */
public class ArgumentUtils
{
  private ArgumentUtils() {
  }

  public static void checkArgument(boolean expression) throws IllegalArgumentException {
    checkArgument(null, expression);
  }

  public static void checkArgument(boolean expression, String message) throws IllegalArgumentException {
    checkArgument(null, expression, message);
  }

  public static <T> T checkArgument(T argument, boolean expression) throws IllegalArgumentException {
    return checkArgument(argument, expression, null);
  }

  public static <T> T checkArgument(T argument, boolean expression, String message) throws IllegalArgumentException {
    if (!expression) {
      throw isNotBlank(message) ? new IllegalArgumentException(message) : new IllegalArgumentException();
    }

    return argument;
  }
}
