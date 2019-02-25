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
import java.util.Map;

import com.sonatype.nexus.api.repository.v3.ComponentInfo;
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client;
import com.sonatype.nexus.api.repository.v3.SearchBuilder;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.repository.RepositorySystem;

/**
 * Goal to tag and deploy artifacts to NXRM 3.
 *
 * @since 1.0.0
 */
@Mojo(name = "move", requiresOnline = true, threadSafe = true)
public class StagingMoveMojo
    extends StagingMojo
{
  @Parameter(property = "destinationRepository", required = true)
  private String destinationRepository;

  @Parameter(property = "repository", required = true)
  private String repository;

  @Parameter(property = "tag")
  private String tag;

  @Parameter(property = "sourceRepository")
  private String sourceRepository;

  @Component
  private RepositorySystem repositorySystem;

  @Parameter(defaultValue = "${settings.offline}", readonly = true, required = true)
  private boolean offline;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    doExecute();
  }

  private void doExecute() throws MojoFailureException, MojoExecutionException {
    RepositoryManagerV3Client client = getClientFactory().build(getServerConfiguration(getMavenSession()));

    failIfOffline();

    try {
      tag = determineTagForMoving();

      sourceRepository = determinaSourceRepository();

      getLog().info(String.format("Performing move of artifacts with tag '%s' from '%s' to '%s'",
          tag, sourceRepository, destinationRepository));
      doMove(client, destinationRepository, createSearchCriteria(sourceRepository, tag));
    }
    catch (MojoExecutionException e) {
      throw e;
    }
    catch (Exception ex) {
      throw new MojoFailureException(ex.getMessage(), ex);
    }
  }

  private String determinaSourceRepository() {
    String derivedSourceRepository = null;
    if (sourceRepository == null) {
      //grab the repository from the pom file
      getLog().warn(String.format("No source repository was specified, defaulting to '%s'", "foo"));
      derivedSourceRepository = repository;
    }
    return derivedSourceRepository;
  }

  private String determineTagForMoving() throws MojoExecutionException {
    String derivedTag = null;
    if (tag == null) {
      derivedTag = getTagFromPropertiesFile();
      if (derivedTag == null) {
        getLog().warn("No 'tag' parameter was found but is required for moving artifacts, exiting now");
        throw new MojoExecutionException("The parameter 'tag' is required");
      }
    }
    return derivedTag;
  }

  private void doMove(final RepositoryManagerV3Client client,
                        final String desitnationRepository,
                        final Map<String, String> search) throws Exception
  {
    //TODO figure out error messages for the response
    List<ComponentInfo> results = client.move(desitnationRepository, search);

  }

  private Map<String, String> createSearchCriteria(final String repository, final String tag) {
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
}
