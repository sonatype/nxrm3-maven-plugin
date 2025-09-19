/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.nexus.api.repository.v3;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import static java.util.Objects.hash;

/**
 * Simple object which represents a repository in NXRM3
 *
 * @since 3.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Repository
{
  private String name;

  private String format;

  private String type;

  private String url;

  public Repository() {
  }

  public Repository(String name, String format, String type, String url) {
    this.name = name;
    this.format = format;
    this.type = type;
    this.url = url;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(final String format) {
    this.format = format;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Repository that = (Repository) o;
    return Objects.equals(name, that.name) && Objects.equals(format, that.format) && Objects.equals(type, that.type)
        && Objects.equals(url, that.url);
  }

  @Override
  public int hashCode() {
    return hash(name, format, type, url);
  }
}
