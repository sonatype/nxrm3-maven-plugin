/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.nexus.api.repository.v3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.sonatype.nexus.api.common.ArgumentUtils.checkArgument;
import static org.sonatype.nexus.api.common.NexusStringUtils.isNotBlank;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;

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
