/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.nexus.api.repository.v3.impl.rest;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.api.repository.v3.ComponentInfo;
import org.sonatype.nexus.api.repository.v3.rest.RestResponse;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.StatusLine;

import static java.util.Collections.emptyList;

/**
 * Simple {@link org.apache.http.client.ResponseHandler} that parses a Nexus Repository Manager 3.x REST response for
 * a list of {@link ComponentInfo}
 *
 * @since 3.1
 */
public class ComponentInfoResponseHandler
    extends NxrmResponseHandler<List<ComponentInfo>>
{
  private static final Gson GSON = new Gson();

  private static final Type COMPONENT_INFO_LIST_TYPE = new TypeToken<List<ComponentInfo>>()
  {
  }.getType();

  private final String componentListKey;

  ComponentInfoResponseHandler(final String componentListKey) {
    this.componentListKey = componentListKey;
  }

  public String getComponentListKey() {
    return componentListKey;
  }

  @Override
  protected List<ComponentInfo> handle(final String responseBody, final StatusLine statusLine) {
    Map<String, Object> tagResponseData = RestResponse.parseJson(responseBody).getData();

    if (tagResponseData == null || !tagResponseData.containsKey(componentListKey)) {
      return emptyList();
    }

    return GSON.fromJson(GSON.toJson(tagResponseData.get(componentListKey)), COMPONENT_INFO_LIST_TYPE);
  }
}
