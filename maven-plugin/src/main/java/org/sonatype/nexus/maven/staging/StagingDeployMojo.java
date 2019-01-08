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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sonatype.nexus.api.exception.RepositoryManagerException;
import com.sonatype.nexus.api.repository.v3.DefaultAsset;
import com.sonatype.nexus.api.repository.v3.DefaultComponent;
import com.sonatype.nexus.api.repository.v3.Repository;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.repository.RepositorySystem;

@Mojo(name = "deploy", requiresOnline = true, threadSafe = true)
public class StagingDeployMojo
    extends StagingMojo
{

  private static final String FORMAT = "maven2";

  @Parameter(property = "repository", required = true)
  private String repository;

  @Parameter(property = "tag")
  private String tag;

  @Parameter(property = "tagFile")
  private File tagFile;

  final String tagUrl = "/tag";

  final String searchUrl = "/search";

  /**
   * Component used to create an artifact.
   */
  @Component
  private RepositorySystem repositorySystem;

  /**
   * Maven Session.
   */
  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession mavenSession;

  /**
   * Project Artifact.
   */
  @Parameter(defaultValue = "${project.artifact}", readonly = true, required = true)
  private Artifact artifact;

  /**
   * Project packaging.
   */
  @Parameter(defaultValue = "${project.packaging}", readonly = true, required = true)
  private String packaging;

  /**
   * Project POM file.
   */
  @Parameter(defaultValue = "${project.file}", readonly = true, required = true)
  private File pomFile;

  /**
   * Project's attached artifacts.
   */
  @Parameter(defaultValue = "${project.attachedArtifacts}", readonly = true, required = true)
  private List<Artifact> attachedArtifacts;

  /**
   * Parameter used to update the metadata to mark the artifact as release.
   */
  @Parameter(property = "updateReleaseInfo")
  private boolean updateReleaseInfo;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    // StagingV2 cannot work offline, it needs REST calls to talk to Nexus even if not
    // deploying remotely (for example skipRemoteStaging equals true), but for stuff like profile selection,
    // matching, etc.
    //failIfOffline();

    //Prepare deployment
    List<DeployableArtifact> deployables = prepareDeployables();

    //Note: Need to handle the case where a tag and tagfile exists...
    try {
      if (tag != null && !tag.isEmpty()) {
        getLog().info(String.format("Deploying to repository '%s' with tag '%s'", repository, tag));
        doUpload(deployables, tag);
      }
      else if (tagFile != null) {
        getLog().info(String.format("Deploying to repository '%s' with tag from file '%s'", repository, tagFile));
      }
      else {
        throw new MojoExecutionException("One of the parameters 'tag' or 'tagFile' are required");
      }
    }
    catch (Exception ex) {
      throw new MojoFailureException(ex.getMessage(), ex);
    }
  }

  protected MavenSession getMavenSession() {
    return mavenSession;
  }

  private DefaultComponent getDefaultComponent(DeployableArtifact deployableArtifact) throws FileNotFoundException {
    DefaultComponent component = new DefaultComponent(FORMAT);
    Artifact baseArtifact = deployableArtifact.getArtifact();
    component.addAttribute("version", baseArtifact.getBaseVersion());
    component.addAttribute("groupId", baseArtifact.getGroupId());
    component.addAttribute("artifactId", baseArtifact.getArtifactId());
    return component;
  }

  private void doUpload(final List<DeployableArtifact> deployables, final String tag) throws Exception {
    DefaultComponent component = getDefaultComponent(deployables.get(0));

    //Add all assets
    for (DeployableArtifact artifact: deployables) {
      Artifact tempArtifact = artifact.getArtifact();
      DefaultAsset asset = new DefaultAsset(tempArtifact.getFile().getName(), new FileInputStream(tempArtifact.getFile()));
      asset.addAttribute("extension", tempArtifact.getType());

      if (tempArtifact.getClassifier() != null) {
        asset.addAttribute("classifier", tempArtifact.getClassifier());
      }
      component.addAsset(asset);
    }

    getClient().upload(repository, component, tag);
  }

  private List<DeployableArtifact> prepareDeployables() throws MojoExecutionException {
    // DEPLOY
    final List<DeployableArtifact> deployables = new ArrayList<>(2);

    // Deploy the POM
    boolean isPomArtifact = "pom".equals(packaging);
    if (!isPomArtifact) {
      artifact.addMetadata(new ProjectArtifactMetadata(artifact, pomFile));
    }

    if (isPomArtifact) {
      deployables.add(new DeployableArtifact(pomFile, artifact));
    }
    else {
      final File file = artifact.getFile();

      if (file != null && file.isFile()) {
        deployables.add(new DeployableArtifact(file, artifact));
      }
      else if (!attachedArtifacts.isEmpty()) {
        getLog().info("No primary artifact to deploy, deploying attached artifacts instead.");

        final Artifact pomArtifact =
            repositorySystem.createProjectArtifact(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getBaseVersion());
        pomArtifact.setFile(pomFile);
        if (updateReleaseInfo) {
          pomArtifact.setRelease(true);
        }

        deployables.add(new DeployableArtifact(pomFile, pomArtifact));

        // propagate the timestamped version to the main artifact for the attached artifacts to pick it up
        artifact.setResolvedVersion(pomArtifact.getVersion());
      }
      else {
        throw new MojoExecutionException(
            "The packaging for this project did not assign a file to the build artifact");
      }
    }

    for (Iterator<Artifact> i = attachedArtifacts.iterator(); i.hasNext(); ) {
      Artifact attached = i.next();
      deployables.add(new DeployableArtifact(attached.getFile(), attached));
    }

    return deployables;
  }

  private void listRepos() {
    List<Repository> repos = null;
    try {
      repos = getClient().getRepositories();
      for (Repository repo : repos) {
        System.out
            .println(String.format("Format %s Type %s Name %s", repo.getFormat(), repo.getType(), repo.getName()));
      }
    }
    catch (RepositoryManagerException e) {
      e.printStackTrace();
    }
  }

}
