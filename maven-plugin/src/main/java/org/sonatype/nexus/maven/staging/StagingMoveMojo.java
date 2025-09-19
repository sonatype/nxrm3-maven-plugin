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

import org.sonatype.nexus.api.exception.RepositoryManagerException;
import org.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client;
import org.sonatype.nexus.api.repository.v3.SearchBuilder;

import com.google.common.annotations.VisibleForTesting;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static java.lang.String.format;

/**
 * Goal to move artifacts between repositories in NXRM 3.
 *
 * @since 1.0.0
 */
@Mojo(name = "staging-move", requiresOnline = true, requiresDirectInvocation=true, requiresProject=false)
public class StagingMoveMojo
    extends StagingActionMojo
{
  @Parameter(property = "repository", required = true)
  private String repository;

  @Parameter(property = "destinationRepository", required = true)
  private String destinationRepository;

  @Parameter(property = "sourceRepository")
  private String sourceRepository;

  @Override
  public void execute() throws MojoFailureException {
    RepositoryManagerV3Client client = getRepositoryManagerV3Client();

    failIfOffline();

    if (destinationRepository == null || destinationRepository.isEmpty()) {
      throw new MojoFailureException("'destinationRepository' is required but was not found");
    }

    try {
      tag = getTag();

      sourceRepository = getSourceRepository();

      getLog().info(format("Moving artifacts with tag '%s' from '%s' to '%s'", tag, sourceRepository,
          destinationRepository));

      client.move(destinationRepository, createSearchCriteria(sourceRepository, tag));
    }
    catch (RepositoryManagerException e) {
      String reason = format("%s. Reason: %s", e.getMessage(), e.getResponseMessage().isPresent() ?
          e.getResponseMessage().get() : e.getCause().getMessage());

      throw new MojoFailureException(reason, e);
    }
    catch (Exception e) {
      throw new MojoFailureException(e.getMessage(), e);
    }
  }

  @VisibleForTesting
  String getSourceRepository() {
    if (sourceRepository == null || sourceRepository.isEmpty()) {
      getLog().info(format("No source repository was specified, defaulting to '%s'", repository));
      sourceRepository = repository;
    }
    return sourceRepository;
  }

  @VisibleForTesting
  Map<String, String> createSearchCriteria(final String repository, final String tag) {
    return SearchBuilder.create().withRepository(repository).withTag(tag).build();
  }

  @VisibleForTesting
  void setSourceRepository(final String sourceRepository) {
    this.sourceRepository = sourceRepository;
  }

  @VisibleForTesting
  void setDestinationRepository(final String destinationRepository) {
    this.destinationRepository = destinationRepository;
  }
}
