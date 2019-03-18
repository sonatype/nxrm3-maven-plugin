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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import com.google.common.annotations.VisibleForTesting;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Parent for all "CLI" staging MOJOs (goals) which can be configured from the CLI (or pom) but do not
 * require a project.
 *
 * @since 1.0.0
 */
public abstract class StagingActionMojo
  extends StagingMojo
{
  @Parameter(property = "tag")
  protected String tag;

  @VisibleForTesting
  String getTag() throws MojoExecutionException {
    if (tag == null || tag.isEmpty()) {
      tag = getTagFromPropertiesFile();
    }
    return tag;
  }

  @VisibleForTesting
  void setTag(final String tag) {
    this.tag = tag;
  }

  private String getTagFromPropertiesFile() throws MojoExecutionException {
    Properties stagingProperties = loadStagingProperties();
    return Optional.ofNullable(stagingProperties.getProperty(TAG_ID))
        .filter(s -> !s.isEmpty())
        .orElseThrow(() -> new MojoExecutionException("Property 'staging.tag' is either not defined or is empty in " +
            "staging properties file"));
  }

  private Properties loadStagingProperties() throws MojoExecutionException {
    Properties properties = new Properties();
    try (InputStream inputStream = new FileInputStream(getStagingPropertiesFile())) {
      properties.load(inputStream);
    }
    catch (IOException e) { //NOSONAR
      getLog().error(e.getMessage());
      throw new MojoExecutionException("Encountered an error while accessing 'staging.tag' " +
          "property from staging properties file: " + getStagingPropertiesFile());
    }
    return properties;
  }
}
