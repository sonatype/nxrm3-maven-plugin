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
package org.sonatype.nexus.api.repository.v3;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.sonatype.nexus.api.common.ArgumentUtils.checkArgument;
import static org.sonatype.nexus.api.common.NexusStringUtils.isNotBlank;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * Default implementation for a NXRM3 {@link Asset}. Asset attributes will automatically be keyed
 * appropriately
 *
 * @since 3.0
 */
public class DefaultAsset
    implements Asset
{
  private final String filename;

  private final InputStream data;

  private final Map<String, String> attributes = new HashMap<>();

  public DefaultAsset(String filename, InputStream data) {
    this.filename = checkArgument(filename, isNotBlank(filename), "Filename is required");
    this.data = requireNonNull(data, "Asset payload is required");
  }

  @Override
  @Deprecated
  public String getName() {
    return null;
  }

  @Override
  public String getFilename() {
    return filename;
  }

  @Override
  public InputStream getData() {
    return data;
  }

  @Override
  public Map<String, String> getAttributes() {
    return unmodifiableMap(attributes);
  }

  /**
   * Add an attribute to the asset
   *
   * @param name the attribute name, which may not contain a '.' character
   * @param value the attribute value
   */
  public final void addAttribute(final String name, final String value) {
    checkArgument(isNotBlank(name), "Attribute name is required");
    checkArgument(isNotBlank(value), "Attribute value is required");
    checkArgument(!name.contains("."), "Attribute name may not contain the '.' character");

    attributes.put(name, value);
  }

  public final String getAttribute(final String name) {
    return attributes.get(name);
  }
}
