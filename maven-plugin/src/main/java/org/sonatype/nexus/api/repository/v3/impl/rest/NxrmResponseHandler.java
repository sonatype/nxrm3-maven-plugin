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
package org.sonatype.nexus.api.repository.v3.impl.rest;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;

/**
 * Simple response handler similar to {@link org.apache.http.impl.client.BasicResponseHandler}, but will throw a
 * {@link NxrmResponseException} for non 2xx responses. Allows for subclasses to specify if 404 (Not Found) responses
 * should be allowed.
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public abstract class NxrmResponseHandler<T>
    implements ResponseHandler<T>
{
  protected static final boolean ALLOW_NOT_FOUND_STATUS = true;

  private final boolean allowNotFoundStatus;

  protected NxrmResponseHandler() {
    this(!ALLOW_NOT_FOUND_STATUS);
  }

  protected NxrmResponseHandler(boolean allowNotFoundStatus) {
    this.allowNotFoundStatus = allowNotFoundStatus;
  }

  protected abstract T handle(String responseBody, StatusLine statusLine) throws IOException;

  @Override
  public T handleResponse(final HttpResponse response) throws IOException {
    final StatusLine statusLine = response.getStatusLine();
    final HttpEntity entity = response.getEntity();
    final String body = entity != null ? EntityUtils.toString(entity) : null;
    final int statusCode = statusLine.getStatusCode();

    if (statusIsNotAllowed(statusCode)) {
      throw new NxrmResponseException(statusCode, statusLine.getReasonPhrase(), body);
    }

    return handle(body, statusLine);
  }

  private boolean statusIsNotAllowed(int statusCode) {
    return statusCode >= 300 && !(statusCode == SC_NOT_FOUND && allowNotFoundStatus);
  }
}
