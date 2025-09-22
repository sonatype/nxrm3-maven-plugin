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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static org.sonatype.nexus.api.common.ArgumentUtils.checkArgument;
import static org.sonatype.nexus.api.common.NexusStringUtils.isNotBlank;

/**
 * Represents a component for upload to NXRM3. A component is a collection of {@link Asset}s and associated
 * metadata
 *
 * @since 3.0
 */
public class DefaultComponent
    implements Component
{
  private final String format;

  private Collection<Asset> assets = new ArrayList<>();

  private Map<String, String> attributes = new HashMap<>();

  /**
   * @param format the format type of the component, such as 'maven2' or 'raw'
   */
  public DefaultComponent(final String format) {
    this.format = format;
  }

  @Override
  public String getFormat() {
    return format;
  }

  @Override
  public Collection<Asset> getAssets() {
    return unmodifiableCollection(assets);
  }

  public void setAssets(final Collection<Asset> assets) {
    this.assets = assets;
  }

  public void addAsset(final Asset asset) {
    assets.add(asset);
  }

  @Override
  public Map<String, String> getAttributes() {
    return unmodifiableMap(attributes);
  }

  public void addAttribute(final String attrName, final String value) {
    checkArgument(isNotBlank(attrName), "Attribute name is required");
    checkArgument(isNotBlank(value), "Attribute value is required");

    // validate attribute name
    String nameToValidate = attrName;
    String format = getFormat();

    if (isNotBlank(format)) {
      String prefix = format + ".";
      if (nameToValidate.startsWith(prefix)) {
        nameToValidate = nameToValidate.substring(prefix.length() + 1);
      }
    }
    checkArgument(!nameToValidate.contains("."), "Invalid attribute name");

    attributes.put(attrName, value);
  }

  public void removeAttribute(final String name) {
    attributes.remove(name);
  }
}
