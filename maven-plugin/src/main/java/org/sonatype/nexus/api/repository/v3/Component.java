/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.nexus.api.repository.v3;

import java.util.Collection;
import java.util.Map;

/**
 * Defines the basic operations for a component that will be uploaded to NXRM3.
 * See <a href="https://help.sonatype.com/display/NXRM3/Components+API">Components API</a> for details
 *
 * @since 3.0
 */
public interface Component
{
  /**
   * @return the format name for the component (such as 'maven2' or 'raw').
   */
  String getFormat();

  /**
   * @return the collection of assets for the component
   */

  Collection<Asset> getAssets();

  /**
   * @return the attributes of the component
   */
  Map<String, String> getAttributes();
}
