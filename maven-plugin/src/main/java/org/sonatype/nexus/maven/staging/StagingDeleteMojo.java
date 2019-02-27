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

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.google.common.annotations.VisibleForTesting;
import com.sonatype.nexus.api.repository.v3.ComponentInfo;
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client;

/**
 * Goal to delete artifacts from NXRM 3.
 *
 * @since 1.0.0
 */
@Mojo(name = "delete", requiresOnline = true, threadSafe = true, requiresDirectInvocation=true, requiresProject=false)
public class StagingDeleteMojo
    extends StagingMojo
{
  @Parameter(property = "tag")
  private String tag;

  @Parameter(defaultValue = "${settings.offline}", readonly = true, required = true)
  private boolean offline;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    failIfOffline();

    if (tag == null || tag.isEmpty()) {
      String tagFromStagingProperties = readTagFromStagingProperties();
      if (tagFromStagingProperties == null || tagFromStagingProperties.isEmpty()) {
        throw new MojoExecutionException("Property 'staging.tag' is either not defined or is empty in staging properties file");
      }
      tag = tagFromStagingProperties;
    }

    RepositoryManagerV3Client client = getClientFactory().build(getServerConfiguration(getMavenSession()));
    try {
      List<ComponentInfo> deletedComponents = client.delete(tag);
      getLog().info(String.format("Deleted components: %s with tag: %s", deletedComponents, tag));
    }
    catch (Exception ex) {
      throw new MojoFailureException(ex.getMessage(), ex);
    }
  }

  /**
   * Throws {@link MojoFailureException} if Maven is invoked offline, as this plugin MUST WORK online.
   *
   * @throws MojoFailureException if Maven is invoked offline.
   */
  protected void failIfOffline() throws MojoFailureException {
    if (offline) {
      throw new MojoFailureException(
          "Cannot use Staging features in Offline mode, as REST Requests are needed to be made against NXRM");
    }
  }

  @VisibleForTesting
  void setOffline(final boolean offline) {
    this.offline = offline;
  }

  @VisibleForTesting
  void setTag(final String tag) {
    this.tag = tag;
  }
}
