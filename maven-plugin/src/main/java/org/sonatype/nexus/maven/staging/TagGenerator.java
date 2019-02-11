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
package org.sonatype.nexus.maven.staging;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Generates a tag based on the project name, version, and current timestamp.
 *
 * @since 1.0.0
 */
@Singleton
public class TagGenerator
{
  private static final String HYPHEN_DELIMITER = "-";

  private final CurrentTimeSource currentTimeSource;

  @Inject
  public TagGenerator(final CurrentTimeSource currentTimeSource) {
    this.currentTimeSource = currentTimeSource;
  }

  public String generate(final String artifactId, final String projectVersion) {
    return String.join(HYPHEN_DELIMITER, artifactId, projectVersion, Long.toString(currentTimeSource.get()));
  }
}
