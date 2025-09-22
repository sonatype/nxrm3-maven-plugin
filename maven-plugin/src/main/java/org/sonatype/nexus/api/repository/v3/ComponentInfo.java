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

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import static java.util.Objects.hash;
import static java.util.Optional.ofNullable;

/**
 * Basic information regarding a Component stored in NXRM3
 *
 * @since 3.1
 */
public class ComponentInfo
{
  private String group;

  private String name;

  private String version;

  // include for JSON deserialization
  public ComponentInfo() {
  }

  public ComponentInfo(final String group, final String name, final String version) {
    this.group = group;
    this.name = name;
    this.version = version;
  }

  public Optional<String> getGroup() {
    return ofNullable(group);
  }

  public Optional<String> getName() {
    return ofNullable(name);
  }

  public Optional<String> getVersion() {
    return ofNullable(version);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ComponentInfo that = (ComponentInfo) o;

    return Objects.equals(group, that.group) && Objects.equals(name, that.name)
        && Objects.equals(version, that.version);
  }

  @Override
  public int hashCode() {
    return hash(group, name, version);
  }

  @Override
  public String toString() {
    StringJoiner joiner = new StringJoiner(", ", "(", ")");

    getGroup().ifPresent(g -> joiner.add("group: " + g));
    getName().ifPresent(n -> joiner.add("name: " + n));
    getVersion().ifPresent(v -> joiner.add("version: " + v));

    return joiner.toString();
  }
}
