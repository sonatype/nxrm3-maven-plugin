/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.nexus.api.repository.v3;

import java.io.InputStream;
import java.util.Map;

/**
 * Defines the necessary data comprising an asset in NXRM3
 *
 * @since 3.0
 */
public interface Asset
{
  /**
   * @return A name for the asset
   * @deprecated asset name is now handled automatically during upload; this value will be ignored if provided
   */
  @Deprecated
  String getName();

  /**
   * @return The filename of the asset
   */
  String getFilename();

  /**
   * @return Attributes specific to the asset. The key of the attribute value should be prefixed by the asset's name and
   *         a period. For example, with an asset named 'myasset' and an attribute 'isGreat': 'myasset.isGreat'
   */
  Map<String, String> getAttributes();

  /**
   * @return An {@link InputStream} for the asset payload
   */
  InputStream getData();
}
