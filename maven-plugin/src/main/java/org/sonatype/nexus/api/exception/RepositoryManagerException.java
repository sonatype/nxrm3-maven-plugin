/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.nexus.api.exception;

import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

/**
 * A custom exception to indicate when something went wrong while interacting with Nexus Repository Manager
 */
public class RepositoryManagerException
    extends Exception
{
  private static final long serialVersionUID = 5688474593455306151L;

  private final Optional<Integer> responseStatus;

  private final Optional<String> responseMessage;

  /**
   * @param message the details about why the exception occurred.
   */
  public RepositoryManagerException(final String message) {
    super(message);
    this.responseStatus = empty();
    this.responseMessage = empty();
  }

  /**
   * @param message the details about why the exception occurred.
   * @param throwable the underlying cause of the exception.
   */
  public RepositoryManagerException(final String message, final Throwable throwable) {
    super(message, throwable);
    this.responseStatus = empty();
    this.responseMessage = empty();
  }

  /**
   * @param message the details about why the exception occurred.
   * @param responseStatus HTTP status code of the Nexus Repository Manager response
   */
  public RepositoryManagerException(final String message, final int responseStatus) {
    super(message);
    this.responseStatus = of(responseStatus);
    this.responseMessage = empty();
  }

  /**
   * @param message the details about why the exception occurred.
   * @param responseStatus HTTP status code of the Nexus Repository Manager response
   * @param responseMessage Detail message of the Nexus Repository Manager response
   */
  public RepositoryManagerException(
      final String message,
      final int responseStatus,
      final String responseMessage)
  {
    super(message);
    this.responseStatus = of(responseStatus);
    this.responseMessage = ofNullable(responseMessage);
  }

  /**
   * @param message the details about why the exception occurred.
   * @param throwable the underlying cause of the exception.
   * @param responseStatus HTTP status code of the Nexus Repository Manager response
   */
  public RepositoryManagerException(final String message, final Throwable throwable, final int responseStatus) {
    super(message, throwable);
    this.responseStatus = of(responseStatus);
    this.responseMessage = empty();
  }

  /**
   * @param message the details about why the exception occurred.
   * @param throwable the underlying cause of the exception.
   * @param responseStatus HTTP status code of the Nexus Repository Manager response
   * @param responseMessage Detail message of the Nexus Repository Manager response
   */
  public RepositoryManagerException(
      final String message,
      final Throwable throwable,
      final int responseStatus,
      final String responseMessage)
  {
    super(message, throwable);
    this.responseStatus = of(responseStatus);
    this.responseMessage = ofNullable(responseMessage);
  }

  public Optional<Integer> getResponseStatus() {
    return responseStatus;
  }

  public Optional<String> getResponseMessage() {
    return responseMessage;
  }
}
