/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.nexus.api.repository.v3.impl.rest;

import java.io.IOException;

import org.sonatype.nexus.api.repository.v3.NxrmVersion;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.http.StatusLine;

/**
 * Simple response handler to parse NXRM3 status version endpoint response
 *
 * @since 3.4
 */
public class GetVersionResponseHandler
    extends NxrmResponseHandler<NxrmVersion>
{
  /**
   * Parses the response body as a {@link NxrmVersion} object.
   */
  @Override
  protected NxrmVersion handle(final String responseBody, final StatusLine statusLine) throws IOException {
    try {
      XmlMapper xmlMapper = new XmlMapper(new WstxInputFactory(), new WstxOutputFactory());
      return xmlMapper.readValue(responseBody, NxrmVersion.class);
    }
    catch (Exception e) {
      throw new IOException(e);
    }
  }
}
