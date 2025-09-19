/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
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
