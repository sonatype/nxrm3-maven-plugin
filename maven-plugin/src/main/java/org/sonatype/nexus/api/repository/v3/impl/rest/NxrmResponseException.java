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

import java.util.Optional;

import org.sonatype.nexus.api.repository.v3.rest.RestResponse;

import org.apache.http.client.HttpResponseException;

import static java.util.Optional.ofNullable;

/**
 * Exception indicating a non 2xx response from NXRM3 that includes the response body if available
 */
public class NxrmResponseException
    extends HttpResponseException
{
  private static final long serialVersionUID = 9118548495994857578L;

  private final String responseBody;

  public NxrmResponseException(final int statusCode, final String s, final String responseBody) {
    super(statusCode, s);
    this.responseBody = responseBody;
  }

  public Optional<String> getResponseBody() {
    return ofNullable(responseBody);
  }

  public Optional<String> getNxrmMessage() {
    return ofNullable(getResponseBody().map(body -> {
      try {
        return RestResponse.parseJson(body).getMessage();
      }
      catch (Exception ex) {
        // noop
        return null;
      }
    }).orElse(null));
  }
}
