/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.nexus.api.exception;

public class IqClientException
    extends Exception
{
  private static final long serialVersionUID = -6052347002753197225L;

  /**
   *
   * @param message the details about why the exception occurred.
   */
  public IqClientException(final String message) {
    super(message);
  }

  /**
   *
   * @param message the details about why the exception occured.
   * @param throwable the underlying cause of the exception.
   */
  public IqClientException(final String message, final Throwable throwable) {
    super(message, throwable);
  }
}
