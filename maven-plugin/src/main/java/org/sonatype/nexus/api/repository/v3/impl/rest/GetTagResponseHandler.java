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

import org.sonatype.nexus.api.repository.v3.Tag;
import org.sonatype.nexus.api.repository.v3.impl.rest.NxrmResponseHandler;

import com.google.gson.Gson;
import org.apache.http.StatusLine;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;

/**
 * Simple {@link org.apache.http.client.ResponseHandler} that parses a Nexus Repository Manager 3.x REST response for
 * a {@link Tag}. Allows for 404 (Not Found) by returning a {@link Optional}
 *
 * @since 3.1
 */
public class GetTagResponseHandler
    extends NxrmResponseHandler<Optional<Tag>>
{
  private static final Gson GSON = new Gson();

  public GetTagResponseHandler() {
    super(ALLOW_NOT_FOUND_STATUS);
  }

  @Override
  protected Optional<Tag> handle(final String responseBody, final StatusLine statusLine) {
    return statusLine.getStatusCode() == SC_NOT_FOUND ? Optional.empty()
        : Optional.of(GSON.fromJson(responseBody, Tag.class));
  }
}
