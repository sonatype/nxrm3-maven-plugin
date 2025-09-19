/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.nexus.api.repository.v3.impl.rest;

import java.io.IOException;
import java.util.List;

import org.sonatype.nexus.api.repository.v3.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.http.StatusLine;

/**
 * Simple response handler to parse NXRM 3.x repositories endpoint response
 *
 * @since 3.0
 */
public class GetRepositoriesResponseHandler
    extends NxrmResponseHandler<List<Repository>>
{
  private static final ObjectReader READER = new ObjectMapper().readerFor(new TypeReference<List<Repository>>()
  {
  });

  /**
   * Parses the response body as a {@link List} of {@link Repository} objects
   */
  @Override
  protected List<Repository> handle(final String responseBody, final StatusLine statusLine) throws IOException {
    return READER.readValue(responseBody);
  }
}
