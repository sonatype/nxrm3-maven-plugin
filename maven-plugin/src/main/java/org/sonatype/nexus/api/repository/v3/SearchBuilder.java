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

import java.util.HashMap;
import java.util.Map;

import static org.sonatype.nexus.api.common.ArgumentUtils.checkArgument;
import static org.sonatype.nexus.api.common.NexusStringUtils.isNotBlank;

/**
 * Builder to construct NXRM3 component search parameters for a HTTP request
 *
 * @see <a href="https://help.sonatype.com/display/NXRM3/Search+API">Search API</a>
 * @since 3.1
 */
public class SearchBuilder
{
  static final String KEYWORD = "q";

  static final String GROUP = "group";

  static final String NAME = "name";

  static final String VERSION = "version";

  static final String FORMAT = "format";

  static final String REPOSITORY = "repository";

  static final String TAG = "tag";

  private Map<String, String> searchParameters = new HashMap<>();

  private SearchBuilder() {
  }

  /**
   * @return a new {@code SearchBuilder} instance
   */
  public static SearchBuilder create() {
    return new SearchBuilder();
  }

  /**
   * @return a map containing the configured search parameters
   */
  public Map<String, String> build() {
    return new HashMap<>(searchParameters);
  }

  /**
   * Add a search parameter. If the parameter name already exists its value will be overwritten
   *
   * @param name search parameter name (required)
   * @param value search parameter value (required)
   * @return {@code this} for method chaining
   */
  public SearchBuilder withParameter(final String name, final String value) {
    searchParameters.put(checkArgument(name, isNotBlank(name), "Search parameter name is required"),
        checkArgument(value, isNotBlank(value), "Search parameter value is required"));
    return this;
  }

  /**
   * Sets the keyword option for the search
   *
   * @param keyword non-null value
   * @return {@code this} for method chaining
   */
  public SearchBuilder withKeyword(final String keyword) {
    return withParameter(KEYWORD, keyword);
  }

  /**
   * Sets the group option for the search
   *
   * @param group non-null value
   * @return {@code this} for method chaining
   */
  public SearchBuilder withGroup(final String group) {
    return withParameter(GROUP, group);
  }

  /**
   * Sets the name option for the search
   *
   * @param name non-null value
   * @return {@code this} for method chaining
   */
  public SearchBuilder withName(final String name) {
    return withParameter(NAME, name);
  }

  /**
   * Sets the version option for the search
   *
   * @param version non-null value
   * @return {@code this} for method chaining
   */
  public SearchBuilder withVersion(final String version) {
    return withParameter(VERSION, version);
  }

  /**
   * Sets the format option for the search
   *
   * @param format non-null value
   * @return {@code this} for method chaining
   */
  public SearchBuilder withFormat(final String format) {
    return withParameter(FORMAT, format);
  }

  /**
   * Sets the repository option for the search
   *
   * @param repository non-null value
   * @return {@code this} for method chaining
   */
  public SearchBuilder withRepository(final String repository) {
    return withParameter(REPOSITORY, repository);
  }

  /**
   * Sets the tag option for the search
   *
   * @param tagName non-null value
   * @return {@code this} for method chaining
   */
  public SearchBuilder withTag(final String tagName) {
    return withParameter(TAG, tagName);
  }
}
