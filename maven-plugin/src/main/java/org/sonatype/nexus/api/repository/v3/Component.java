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
