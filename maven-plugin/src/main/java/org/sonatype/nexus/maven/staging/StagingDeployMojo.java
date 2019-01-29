/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-present Sonatype, Inc.
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import com.sonatype.nexus.api.exception.RepositoryManagerException;
import com.sonatype.nexus.api.repository.v3.DefaultAsset;
import com.sonatype.nexus.api.repository.v3.DefaultComponent;
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client;
import com.sonatype.nexus.api.repository.v3.Tag;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.repository.RepositorySystem;

/**
 * Goal to tag and deploy artifacts to NXRM 3.
 *
 * @since 3.next
 */
@Mojo(name = "staging-deploy", requiresOnline = true, threadSafe = true)
public class StagingDeployMojo
    extends StagingMojo
{
  private static final String FORMAT = "maven2";

  private static final String TAG_ID = "staging.tag";

  private static final String STAGING_PROPERTIES_FILENAME = "staging.properties";

  @Parameter(property = "repository", required = true)
  private String repository;

  @Parameter(property = "tag")
  private String tag;

  @Component
  private RepositorySystem repositorySystem;

  @Parameter(defaultValue = "${project.artifact}", readonly = true, required = true)
  private Artifact artifact;

  @Parameter(defaultValue = "${project.packaging}", readonly = true, required = true)
  private String packaging;

  @Parameter(defaultValue = "${project.file}", readonly = true, required = true)
  private File pomFile;

  @Parameter(defaultValue = "${project.attachedArtifacts}", readonly = true, required = true)
  private List<Artifact> attachedArtifacts;

  @Parameter(defaultValue = "${settings.offline}", readonly = true, required = true)
  private boolean offline;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    RepositoryManagerV3Client client = getClient(getMavenSession());

    failIfOffline();

    List<Artifact> deployables = prepareDeployables();

    try {
      if (tag != null && !tag.isEmpty()) {
        maybeCreateTag(client, tag);
        getLog().info(String.format("Deploying to repository '%s' with tag '%s'", repository, tag));
        doUpload(client, deployables, tag);
      }
      else {
        throw new MojoExecutionException("The parameters 'tag' is required");
      }
    }
    catch (Exception ex) {
      throw new MojoFailureException(ex.getMessage(), ex);
    }
  }

  private void maybeCreateTag(final RepositoryManagerV3Client client, final String tag)
      throws RepositoryManagerException
  {
    Optional<Tag> existingTag = client.getTag(tag);

    if (!existingTag.isPresent()) {
      getLog().info(String.format("Creating tag %s as it does not already exist", tag));
      client.createTag(tag);
    }
    else {
      getLog().info(String.format("Tag %s  already exists, skipping creation", tag));
    }

    storeTagInPropertiesFile(tag);
  }

  private void storeTagInPropertiesFile(final String tag) {
    final Properties stagingProperties = new Properties();
    stagingProperties.put(TAG_ID, tag);

    final File stagingPropertiesFile = new File(getStagingDirectoryRoot(), STAGING_PROPERTIES_FILENAME);

    if (!stagingPropertiesFile.getParentFile().isDirectory()) {
      stagingPropertiesFile.getParentFile().mkdirs();
    }

    try (OutputStream out = new FileOutputStream(stagingPropertiesFile)) {
      getLog().info(String.format("Saving staging information to %s", stagingPropertiesFile.getAbsolutePath()));

      stagingProperties.store(out, "NXRM3 Maven staging plugin");
    }
    catch (IOException e) {
      getLog().error(e);
    }
  }

  private DefaultComponent getDefaultComponent(Artifact artifact) {
    DefaultComponent component = new DefaultComponent(FORMAT);
    component.addAttribute("version", artifact.getBaseVersion());
    component.addAttribute("groupId", artifact.getGroupId());
    component.addAttribute("artifactId", artifact.getArtifactId());
    return component;
  }

  private void doUpload(final RepositoryManagerV3Client client,
                        final List<Artifact> deployables,
                        final String tag) throws Exception
  {
    DefaultComponent component = getDefaultComponent(deployables.get(0));

    for (Artifact artifact : deployables) {
      DefaultAsset asset = new DefaultAsset(artifact.getFile().getName(), new FileInputStream(artifact.getFile()));

      asset.addAttribute("extension", artifact.getType());

      if (artifact.getClassifier() != null) {
        asset.addAttribute("classifier", artifact.getClassifier());
      }
      component.addAsset(asset);
    }

    client.upload(repository, component, tag);
  }

  private List<Artifact> prepareDeployables() throws MojoExecutionException {
    final List<Artifact> deployables = new ArrayList<>();

    if ("pom".equals(packaging)) {
      deployables.add(artifact);
    }
    else {
      artifact.addMetadata(new ProjectArtifactMetadata(artifact, pomFile));
      
      final File file = artifact.getFile();

      if (file != null && file.isFile()) {
        deployables.add(artifact);
      }
      else if (!attachedArtifacts.isEmpty()) {
        getLog().info("No primary artifact to deploy, deploying attached artifacts instead.");

        final Artifact pomArtifact =
            repositorySystem.createProjectArtifact(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getBaseVersion());
        pomArtifact.setFile(pomFile);

        deployables.add(pomArtifact);
        
        artifact.setResolvedVersion(pomArtifact.getVersion());
      }
      else {
        throw new MojoExecutionException("The packaging for this project did not assign a file to the build artifact");
      }
    }

    deployables.addAll(attachedArtifacts);

    return deployables;
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
