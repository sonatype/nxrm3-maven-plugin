/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2018 Sonatype, Inc.
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

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "deploy", requiresOnline = true)
public class StagingDeployMojo
    extends AbstractMojo
{
  @Parameter(property = "repository", required = true)
  private String repository;

  @Parameter(property = "tag")
  private String tag;

  @Parameter(property = "tagFile")
  private File tagFile;

  @Override
  public void execute() throws MojoExecutionException {
    if (tag != null && !tag.isEmpty()) {
      getLog().info("Deploying to repository " + repository + " with tag " + tag);
    }
    else if (tagFile != null) {
      getLog().info("Deploying to repository " + repository + " with tag from file " + tagFile);
    }
    else {
      throw new MojoExecutionException("One of the parameters 'tag' or 'tagFile' are required");
    }
  }
}
