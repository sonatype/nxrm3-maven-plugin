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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.api.common.ArgumentUtils.checkArgument;
import static org.sonatype.nexus.api.common.NexusStringUtils.isNotBlank;

/**
 * Represents a tag in NXRM3. Tags have a name (must be unique) and optional attributes. The NXRM3 server defines a
 * maximum size for tag attributes.
 *
 * @since 3.1
 */
public class Tag
{
  private static final Gson JSON_WRITER = new Gson();

  private String name;

  private Date firstCreated;

  private Date lastUpdated;

  private Map<String, Object> attributes;

  // include for JSON deserialization
  public Tag() {
  }

  /**
   * Creates a tag with empty attributes.
   *
   * @param name the unique name for the tag
   */
  public Tag(final String name) {
    this(name, new HashMap<>());
  }

  /**
   * Creates a tag with the specified attributes.
   *
   * @param name the unique name for the tag
   * @param attributes optional map of attributes
   */
  public Tag(final String name, final Map<String, Object> attributes) {
    this.name = checkArgument(name, isNotBlank(name), "Tag name is required");
    this.attributes = requireNonNull(attributes, "Attributes are required");
  }

  public Tag(final String name, final Map<String, Object> attributes, final Date firstCreated, final Date lastUpdated) {
    this(name, attributes);
    this.firstCreated = firstCreated;
    this.lastUpdated = lastUpdated;
  }

  /**
   * @return the tag name
   */
  public String getName() {
    return name;
  }

  /**
   * @return a read-only view of the tag attributes
   */
  public Map<String, Object> getAttributes() {
    return unmodifiableMap(attributes);
  }

  /**
   * Adds an attribute to the tag's metadata. If the attribute already exists its value will be replaced.
   *
   * @param name the name of the attribute
   * @param value the attribute value
   */
  public void addAttribute(final String name, final Object value) {
    attributes.put(checkArgument(name, isNotBlank(name), "Attribute name is required"), value);
  }

  /**
   * Removes an attribute from the tag's metadata.
   *
   * @param name the attribute name
   */
  public void removeAttribute(final String name) {
    attributes.remove(name);
  }

  /**
   * @return the tag first created date
   */
  public Date getFirstCreated() {
    return firstCreated;
  }

  /**
   * @return the tag last updated date
   */
  public Date getLastUpdated() {
    return lastUpdated;
  }

  /**
   * @return the JSON representation of this tag
   */
  public String toJson() {
    return JSON_WRITER.toJson(this);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Tag that = (Tag) o;
    return Objects.equals(name, that.name) && Objects.equals(attributes, that.attributes)
        && Objects.equals(firstCreated, that.firstCreated) && Objects.equals(lastUpdated, that.lastUpdated);
  }

  @Override
  public int hashCode() {
    return hash(name, attributes, firstCreated, lastUpdated);
  }
}
