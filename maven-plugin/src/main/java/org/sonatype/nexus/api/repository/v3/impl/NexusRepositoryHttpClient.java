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
package org.sonatype.nexus.api.repository.v3.impl;

import java.io.IOException;
import java.util.Optional;

import org.sonatype.nexus.api.exception.RepositoryManagerException;
import org.sonatype.nexus.api.repository.v3.impl.rest.NxrmResponseException;
import org.sonatype.nexus.api.repository.v3.impl.rest.NxrmResponseHandler;

import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.protocol.HttpContext;

import static java.util.Objects.requireNonNull;

/**
 * Simple wrapper for a {@link org.apache.http.client.HttpClient} that provides request executions that will wrap
 * exceptions into {@link RepositoryManagerException}s
 *
 * @since 3.0
 */
class NexusRepositoryHttpClient
{
  private static final String GENERIC_REQUEST_NAME = "Request";

  private static final NxrmResponseHandler<Void> NOOP_RESPONSE_HANDLER = new NxrmResponseHandler<Void>()
  {
    @Override
    protected Void handle(final String responseBody, final StatusLine statusLine) {
      return null;
    }
  };

  private final HttpClient delegate;

  public NexusRepositoryHttpClient(final HttpClient delegate) {
    this.delegate = requireNonNull(delegate, "Delegate HttpClient is required");
  }

  public HttpClient httpClient() {
    return delegate;
  }

  /**
   * Executes a {@link HttpUriRequest} with an optional {@link HttpContext} using a
   * {@link BasicResponseHandler}. Exceptions are wrapped into a {@link RepositoryManagerException} and referenced
   * by the supplied request name.
   */
  public void execute(
      final HttpUriRequest request,
      final Optional<HttpContext> context,
      final Optional<String> requestName) throws RepositoryManagerException
  {
    execute(request, NOOP_RESPONSE_HANDLER, context, requestName);
  }

  /**
   * Executes a {@link HttpUriRequest} using the provided {@link ResponseHandler} and optional {@link HttpContext}.
   * Exceptions are wrapped into a {@link RepositoryManagerException} and referenced by the supplied request name.
   */
  public <T> T execute(
      final HttpUriRequest request,
      final NxrmResponseHandler<T> responseHandler,
      final Optional<HttpContext> context,
      final Optional<String> requestName) throws RepositoryManagerException
  {
    requireNonNull(request, "HTTP request is required");
    requireNonNull(responseHandler, "Response handler is required");

    String requestNameStr = requestName.orElse(GENERIC_REQUEST_NAME);

    try {
      return delegate.execute(request, responseHandler, context.orElse(null));
    }
    catch (NxrmResponseException e) {
      throw unsuccessfulEx(requestNameStr, e, e.getNxrmMessage().orElse(null));
    }
    catch (HttpResponseException e) {
      throw unsuccessfulEx(requestNameStr, e, null);
    }
    catch (ClientProtocolException e) {
      throw new RepositoryManagerException(requestNameStr + " was unsuccessful", e);
    }
    catch (IOException e) {
      throw new RepositoryManagerException(requestNameStr + " was unable to complete", e);
    }
  }

  private RepositoryManagerException unsuccessfulEx(
      final String request,
      final HttpResponseException cause,
      final String message) throws RepositoryManagerException
  {
    int statusCode = cause.getStatusCode();
    throw new RepositoryManagerException(request + " was unsuccessful (" + statusCode + " response from server)", cause,
        statusCode, message);
  }
}
