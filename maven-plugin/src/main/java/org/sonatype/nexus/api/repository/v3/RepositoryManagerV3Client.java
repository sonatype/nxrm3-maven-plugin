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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.api.exception.RepositoryManagerException;
import org.sonatype.nexus.api.repository.v3.ComponentInfo;
import org.sonatype.nexus.api.repository.v3.Repository;
import org.sonatype.nexus.api.repository.v3.SearchBuilder;
import org.sonatype.nexus.api.repository.v3.Tag;

/**
 * Defines the supported operations in Nexus Repository Manager 3.x (NXRM3)
 *
 * @since 3.0
 */
public interface RepositoryManagerV3Client
{
  /**
   * Gets the version of the NXRM3 server
   *
   * @return a {@link NxrmVersion} for the server
   * @throws RepositoryManagerException if the request fails; possible causes: unauthorized (401), unauthenticated (403)
   * @since 3.4
   */
  NxrmVersion getVersion() throws RepositoryManagerException;

  /**
   * @return a list of {@link Repository} objects mapped from the congfigured repositories on the target Nexus
   *         Repository Manager server
   * @throws RepositoryManagerException when request fails
   */
  List<Repository> getRepositories() throws RepositoryManagerException;

  /**
   * Uploads a {@link DefaultComponent} to the specified repository in NXRM3
   *
   * @param repositoryName name of the repository to where the component will be uploaded
   * @param component the component being uploaded
   * @throws RepositoryManagerException when upload fails; possible causes: (403) insufficient permissions
   */
  void upload(String repositoryName, Component component) throws RepositoryManagerException;

  /**
   * Uploads a {@link DefaultComponent} to the specified repository in NXRM3 and applies the specified tag
   *
   * @param repositoryName name of the repository to where the component will be uploaded
   * @param component the component being uploaded
   * @param tagName the tag to apply (tag must already exist)
   * @throws RepositoryManagerException when upload fails; possible causes: (403) insufficient permissions, (404) tag
   *           not found
   * @since 3.1
   */
  void upload(String repositoryName, Component component, String tagName) throws RepositoryManagerException;

  /**
   * Gets a tag on NXRM3
   *
   * @param name the name of the tag
   * @return the {@link Optional} object
   * @throws RepositoryManagerException when getting the tag fails. Possible causes: (403) insufficient permissions
   * @since 3.1
   */
  Optional<Tag> getTag(String name) throws RepositoryManagerException;

  /**
   * Creates a tag on NXRM3
   *
   * @param name the unique name for the tag
   * @return the {@link Tag} object
   * @throws RepositoryManagerException when tag creation fails (400). Possible causes: tag name already exists, invalid
   *           characters in tag name, tag attributes are too large
   * @since 3.1
   */
  Tag createTag(String name) throws RepositoryManagerException;

  /**
   * Creates a tag on NXRM3
   *
   * @param name the unique name for the tag
   * @param attributes attribute map for the tag, optional
   * @return the {@link Tag} object
   * @throws RepositoryManagerException when tag creation fails (400). Possible causes: tag name already exists, invalid
   *           characters in tag name, tag attributes are too large
   * @since 3.1
   */
  Tag createTag(String name, Map<String, Object> attributes) throws RepositoryManagerException;

  /**
   * Applies a tag to the component(s) found from the {@link SearchBuilder}
   *
   * @param tagName the name of the tag that should be associated to the component(s)
   * @param searchParameters criteria used to locate components in NXRM3. Must contain at least one parameter
   * @return the list of {@link ComponentInfo} that was associated
   * @throws RepositoryManagerException when association fails because the tag does not exist (404)
   * @since 3.1
   */
  List<ComponentInfo> associate(String tagName, Map<String, String> searchParameters) throws RepositoryManagerException;

  /**
   * Removes a tag from the component(s) found from the {@link SearchBuilder}
   *
   * @param tagName the name of the tag to remove from the component(s)
   * @param searchParameters criteria used to locate components in NXRM3. Must contain at least one parameter
   * @return the list of {@link ComponentInfo} that was disassociated
   * @throws RepositoryManagerException when disassociation fails because the tag does not exist (404)
   * @since 3.1
   */
  List<ComponentInfo> disassociate(
      String tagName,
      Map<String, String> searchParameters) throws RepositoryManagerException;

  /**
   * Moves components to the destination repository based on an associated tag name
   *
   * @param destination the target repository of the move
   * @param tagName the tag name used to locate target components
   * @return a list of moved components
   * @throws RepositoryManagerException if the move fails; possible causes: unauthorized (401), unauthenticated (403),
   *           no components found (404)
   */
  List<ComponentInfo> move(String destination, String tagName) throws RepositoryManagerException;

  /**
   * Moves components to the destination repository based on a component search
   *
   * @param destination the target repository of the move
   * @param searchParameters criteria used to locate components in NXRM3 (must contain at least one parameter)
   * @return a list of moved components
   * @throws RepositoryManagerException if the move fails; possible causes: unauthorized (401), unauthenticated (403),
   *           no components found (404)
   */
  List<ComponentInfo> move(String destination, Map<String, String> searchParameters) throws RepositoryManagerException;

  /**
   * Deletes components from NXRM3 based on an associated tag name
   *
   * @param tagName the tag name used to locate target components
   * @return a list of deleted components
   * @throws RepositoryManagerException if the delete fails; possible causes: unauthorized (401), unauthenticated (403),
   *           no components found (404)
   */
  List<ComponentInfo> delete(String tagName) throws RepositoryManagerException;

  /**
   * Deletes components from NXRM3 based on a component search
   *
   * @param searchParameters criteria used to locate components in NXRM3 (must contain at least one parameter)
   * @return a list of deleted components
   * @throws RepositoryManagerException if the delete fails; possible causes: unauthorized (401), unauthenticated (403),
   *           no components found (404)
   */
  List<ComponentInfo> delete(Map<String, String> searchParameters) throws RepositoryManagerException;
}
