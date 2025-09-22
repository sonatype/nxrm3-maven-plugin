/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2019-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
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
      throw new IllegalArgumentException(message);
    }

    return argument;
  }
}
