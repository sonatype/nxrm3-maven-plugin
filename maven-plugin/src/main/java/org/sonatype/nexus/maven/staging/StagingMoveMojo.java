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

import java.util.Map;

import com.sonatype.nexus.api.exception.RepositoryManagerException;
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client;
import com.sonatype.nexus.api.repository.v3.SearchBuilder;

import com.google.common.annotations.VisibleForTesting;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static java.lang.String.format;

/**
 * Goal to move artifacts between repositories in NXRM 3.
 *
 * @since 1.0.0
 */
@Mojo(name = "move", requiresOnline = true, requiresDirectInvocation=true, requiresProject=false)
public class StagingMoveMojo
    extends StagingMojo
{
  @Parameter(property = "repository", required = true)
  private String repository;

  @Parameter(property = "tag")
  private String tag;

  @Parameter(property = "targetRepository", required = true)
  private String targetRepository;

  @Parameter(property = "sourceRepository")
  private String sourceRepository;

  @Parameter(defaultValue = "${settings.offline}", readonly = true, required = true)
  private boolean offline;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    doExecute();
  }

  private void doExecute() throws MojoFailureException, MojoExecutionException {
    RepositoryManagerV3Client client = getClientFactory().build(getServerConfiguration(getMavenSession()));

    failIfOffline();

    if (targetRepository == null || targetRepository.isEmpty()) {
      throw new MojoFailureException("'targetRepository' is required but was not found");
    }

    try {
      tag = determineTagForMoving();

      sourceRepository = determineSourceRepository();

      getLog().info(format("Moving artifacts with tag '%s' from '%s' to '%s'", tag, sourceRepository, targetRepository));

      client.move(targetRepository, createSearchCriteria(sourceRepository, tag));
    }
    catch (RepositoryManagerException e) {
      throw new MojoExecutionException(format("%s.\n\tReason: %s", e.getMessage(), e.getResponseMessage().get()));
    }
    catch (Exception ex) {
      throw new MojoFailureException(ex.getMessage(), ex);
    }
  }

  @VisibleForTesting
  String determineSourceRepository() {
    //TODO probably needa check on the repository here...
    if (sourceRepository == null || sourceRepository.isEmpty()) {
      getLog().error(format("No source repository was specified, defaulting to '%s' from pom configuration", repository));
      sourceRepository = repository;
    }
    return sourceRepository;
  }

  @VisibleForTesting
  String determineTagForMoving() throws MojoExecutionException {
    if (tag == null || tag.isEmpty()) {
      tag = getTagFromPropertiesFile();
      if (tag == null) {
        getLog().error("No 'tag' parameter was found but one is required for moving artifacts");
        throw new MojoExecutionException("The parameter 'tag' is required");
      }
    }
    return tag;
  }

  @VisibleForTesting
  Map<String, String> createSearchCriteria(final String repository, final String tag) {
    return SearchBuilder.create().withRepository(repository).withTag(tag).build();
  }

  /**
   * Throws {@link MojoFailureException} if Maven is invoked offline, as this plugin MUST WORK online.
   *
   * @throws MojoFailureException if Maven is invoked offline.
   */
  protected void failIfOffline()
      throws MojoFailureException
  {
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

  @VisibleForTesting
  void setSourceRepository(final String sourceRepository) {
    this.sourceRepository = sourceRepository;
  }

  @VisibleForTesting
  void setTargetRepository(final String targetRepository) {
    this.targetRepository = targetRepository;
  }
}
