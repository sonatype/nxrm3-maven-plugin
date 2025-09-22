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
package org.sonatype.nexus.api.repository.v3.rest;

import java.util.Map;

import com.google.gson.Gson;

/**
 * The basic response structure from a REST call to NXRM3
 *
 * @since 3.1
 */
public class RestResponse
{
  private static final Gson GSON = new Gson();

  private int status;

  private String message;

  private Map<String, Object> data;

  public static RestResponse parseJson(final String json) {
    return GSON.fromJson(json, RestResponse.class);
  }

  // include for JSON deserialization
  public RestResponse() {
  }

  public RestResponse(final int status, final String message, final Map<String, Object> data) {
    this.status = status;
    this.message = message;
    this.data = data;
  }

  public int getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public Map<String, Object> getData() {
    return data;
  }
}
