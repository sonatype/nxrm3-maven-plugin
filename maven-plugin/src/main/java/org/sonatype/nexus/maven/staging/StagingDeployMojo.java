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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.sonatype.nexus.api.exception.RepositoryManagerException;
import com.sonatype.nexus.api.repository.v3.DefaultAsset;
import com.sonatype.nexus.api.repository.v3.DefaultComponent;
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client;
import com.sonatype.nexus.api.repository.v3.Tag;

import com.google.common.annotations.VisibleForTesting;
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
 * @since 1.0.0
 */
@Mojo(name = "staging-deploy", requiresOnline = true, threadSafe = true)
public class StagingDeployMojo
    extends StagingMojo
{
  private static final String FORMAT = "maven2";

  @Parameter(property = "repository", required = true)
  private String repository;

  @Parameter(property = "tag")
  private String tag;

  @Parameter(property = "skipNexusStagingDeployMojo")
  private boolean skipNexusStagingDeployMojo;

  @Parameter(property = "stagingMode")
  private String stagingMode;

  @Component
  private RepositorySystem repositorySystem;

  @Inject
  private TagGenerator tagGenerator;

  @Parameter(defaultValue = "${project.artifact}", readonly = true, required = true)
  private Artifact artifact;

  @Parameter(defaultValue = "${project.packaging}", readonly = true, required = true)
  private String packaging;

  @Parameter(defaultValue = "${project.file}", readonly = true, required = true)
  private File pomFile;

  @Parameter(defaultValue = "${project.attachedArtifacts}", readonly = true, required = true)
  private List<Artifact> attachedArtifacts;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skipNexusStagingDeployMojo) {
      getLog().info("Skipping NXRM Staging Deploy Mojo at user's demand.");
      return;
    }
    if (artifact.isSnapshot()) {
      getLog().info("Skipping NXRM Staging Deploy Mojo as " + artifact.getVersion() + " is a SNAPSHOT version");
      return;
    }

    maybeWarnAboutDeprecatedStagingModeProperty();

    doExecute();
  }

  private void doExecute() throws MojoFailureException, MojoExecutionException {
    RepositoryManagerV3Client client = getClientFactory().build(getServerConfiguration(getMavenSession()));

    failIfOffline();

    List<Artifact> deployables = prepareDeployables();

    try {
      tag = getProvidedOrGeneratedTag();
      maybeCreateTag(client, tag);
      getLog().info(String.format("Deploying to repository '%s' with tag '%s'", repository, tag));
      doUpload(client, deployables, tag);
    }
    catch (MojoExecutionException e) {
      throw e;
    }
    catch (Exception ex) {
      throw new MojoFailureException(ex.getMessage(), ex);
    }
  }

  private String getProvidedOrGeneratedTag() {
    if (tag == null || tag.isEmpty()) {
      String generatedTag = tagGenerator.generate(artifact.getArtifactId(), artifact.getBaseVersion());
      getMavenSession().getUserProperties().setProperty("tag", generatedTag);
      getLog().info(String.format("No tag was provided; using generated tag '%s'", generatedTag));
      return generatedTag;
    }

    return tag;
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
      getLog().info(String.format("Tag %s already exists, skipping creation", tag));
    }

    storeTagInPropertiesFile(tag);
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
    List<InputStream> streams = new ArrayList<>();
    
    try {
      DefaultComponent component = getDefaultComponent(deployables.get(0));

      for (Artifact deployableArtifact : deployables) {
        FileInputStream stream = new FileInputStream(deployableArtifact.getFile());
        streams.add(stream);

        DefaultAsset asset = new DefaultAsset(deployableArtifact.getFile().getName(), stream);

        // NEXUS-22246 - just like the maven class DefaultArtifactDeployer use the ArtifactHandler#getExtension()
        asset.addAttribute("extension", deployableArtifact.getArtifactHandler().getExtension());

        if (deployableArtifact.getClassifier() != null) {
          asset.addAttribute("classifier", deployableArtifact.getClassifier());
        }
        component.addAsset(asset);
      }

      client.upload(repository, component, tag);
    }
    finally {
      for (InputStream stream : streams) {
        stream.close();
      }
    }
  }

  private List<Artifact> prepareDeployables() throws MojoExecutionException {
    List<Artifact> deployables = new ArrayList<>();

    boolean pomProject = "pom".equals(packaging);

    if (!pomProject) {
      artifact.addMetadata(new ProjectArtifactMetadata(artifact, pomFile));
    }

    File file = artifact.getFile();

    if (pomProject) {
      getLog().info("Pom project to deploy, deploying with attached artifacts.");
    }
    else if (file != null && file.isFile()) {
      deployables.add(artifact);
    }
    else if (!attachedArtifacts.isEmpty()) {
      getLog().info("No primary artifact to deploy, deploying attached artifacts instead.");
    }
    else {
      throw new MojoExecutionException("The packaging for this project did not assign a file to the build artifact");
    }

    // NEXUS-20029 - we now always add the pom. In the nexus-staging plugin this is done as well but a little different
    // and it adds manually like this or after a certain point using the metadata. Here we chose to add it directly.
    deployables.add(createPomArtifact());
    deployables.addAll(attachedArtifacts);

    return deployables;
  }

  private Artifact createPomArtifact() {
    Artifact pomArtifact = repositorySystem.createProjectArtifact(artifact.getGroupId(), artifact.getArtifactId(),
        artifact.getBaseVersion());

    pomArtifact.setFile(pomFile);

    return pomArtifact;
  }

  private void maybeWarnAboutDeprecatedStagingModeProperty() {
    if (stagingMode != null && !stagingMode.isEmpty()) {
      getLog().warn("The stagingMode property is no longer supported and will be ignored");
    }
  }
  
  @VisibleForTesting
  void setArtifact(final Artifact artifact) {
    this.artifact = artifact;
  }

  @VisibleForTesting
  void setTag(final String tag) {
    this.tag = tag;
  }

  @VisibleForTesting
  void setAttachedArtifacts(final List<Artifact> attachedArtifacts) {
    this.attachedArtifacts = attachedArtifacts;
  }

  @VisibleForTesting
  void setPomFile(final File pomFile) {
    this.pomFile = pomFile;
  }
  
  @VisibleForTesting
  void setPackaging(final String packaging) {
    this.packaging = packaging;
  }

  @VisibleForTesting
  void setSkip(final boolean skip) {
    this.skipNexusStagingDeployMojo = skip;
  }

  @VisibleForTesting
  void setTagGenerator(final TagGenerator tagGenerator) {
    this.tagGenerator = tagGenerator;
  }
}
