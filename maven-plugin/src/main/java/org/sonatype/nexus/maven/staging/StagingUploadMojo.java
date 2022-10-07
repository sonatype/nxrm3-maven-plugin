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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.sonatype.nexus.api.exception.RepositoryManagerException;
import com.sonatype.nexus.api.repository.v3.DefaultAsset;
import com.sonatype.nexus.api.repository.v3.DefaultComponent;
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client;
import com.sonatype.nexus.api.repository.v3.Tag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Goal to tag and upload artifacts to NXRM 3 from a previously deferred deploy (locally staged).
 */
@Mojo(name = "upload", requiresProject = false, requiresOnline = true,
    threadSafe = true)
public class StagingUploadMojo
    extends StagingMojo
{
  private static final String FORMAT = "maven2";

  @Parameter(property = "repository")
  private String repository;

  @Parameter(property = "tag")
  private String tag;

  @Parameter(defaultValue = "${project.artifact}", readonly = true, required = true)
  private Artifact artifact;

  @Inject
  private TagGenerator tagGenerator;

  @Component
  private ArtifactRepositoryFactory artifactRepositoryFactory;

  @Component
  private ArtifactRepositoryLayout artifactRepositoryLayout;

  private final Lock readWriteLock;

  private final ObjectMapper objectMapper;

  private final Log log;

  public StagingUploadMojo() {
    super();
    this.readWriteLock = new ReentrantLock();
    this.objectMapper = new ObjectMapper();
    this.log = getLog();
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Map<DefaultComponent, List<ArtifactInfo>> deployables = prepareDeployables();

    RepositoryManagerV3Client client = getClientFactory().build(getServerConfiguration(getMavenSession()));

    failIfOffline();

    String tagToUse = getTag();
    ensureTagIsSet(client, tagToUse);
    log.info(String.format("Uploading to repository '%s' with tagToUse '%s'", repository, tagToUse));
    uploadComponents(client, deployables, tagToUse);
  }

  private Map<DefaultComponent, List<ArtifactInfo>> prepareDeployables() {
    List<ArtifactInfo> storedArtifacts = readStoredArtifactsFromIndex();
    log.info(String.format("Located %d storedArtifacts", storedArtifacts.size()));
    return groupArtifactsByComponent(storedArtifacts);
  }

  @VisibleForTesting
  List<ArtifactInfo> readStoredArtifactsFromIndex() {
    File index = getStagingIndexFile();
    List<ArtifactInfo> artifacts = new ArrayList<>();
    try {
      readWriteLock.lock();
      if (index.exists()) {
        artifacts.addAll(objectMapper.readValue(index, new TypeReference<List<ArtifactInfo>>() { }));
      }
      else {
        log.warn(String.format("index file not found: %s", index));
      }
    }
    catch (IOException ex) {
      log.error(String.format("Exception whilst reading stored artifacts from index file: %s", index), ex);
    }
    finally {
      readWriteLock.unlock();
    }
    return artifacts;
  }

  private Map<DefaultComponent, List<ArtifactInfo>> groupArtifactsByComponent(final List<ArtifactInfo> artifacts) {
    Map<ArtifactInfoKey, List<ArtifactInfo>> groupedArtifacts = new HashMap<>();

    for (ArtifactInfo info : artifacts) {
      ArtifactInfoKey key = new ArtifactInfoKey(info);
      List<ArtifactInfo> associated = groupedArtifacts.computeIfAbsent(key, k -> new ArrayList<>());
      associated.add(info);
    }

    return groupedArtifacts.entrySet().stream()
        .collect(Collectors.toMap(e -> getDefaultComponent(e.getKey()), Entry::getValue));
  }

  private DefaultComponent getDefaultComponent(final ArtifactInfoKey artifact) {
    DefaultComponent component = new DefaultComponent(FORMAT);
    component.addAttribute("version", artifact.getBaseVersion());
    component.addAttribute("groupId", artifact.getGroupId());
    component.addAttribute("artifactId", artifact.getArtifactId());
    return component;
  }

  private String getTag() {
    if (tag == null || tag.isEmpty()) {
      String generatedTag = tagGenerator.generate(artifact.getArtifactId(), artifact.getBaseVersion());
      getMavenSession().getUserProperties().setProperty("tag", generatedTag);
      log.debug(String.format("No tag was provided; using generated tag '%s'", generatedTag));
      return generatedTag;
    }

    return tag;
  }

  private void ensureTagIsSet(final RepositoryManagerV3Client client, final String tag)
      throws MojoFailureException
  {
    try {
      Optional<Tag> existingTag = client.getTag(tag);

      if (!existingTag.isPresent()) {
        log.debug(String.format("Creating tag '%s' as it does not already exist", tag));
        client.createTag(tag);
      }
      else {
        log.debug(String.format("Tag '%s' already exists, skipping creation", tag));
      }
    }
    catch (RepositoryManagerException ex) {
      log.error(String.format("Unable to create tag '%s': %s", tag, ex.getLocalizedMessage()),
          log.isDebugEnabled() ? ex : null);
      throw new MojoFailureException(ex);
    }
    storeTagInPropertiesFile(tag);
  }

  private void uploadComponents(
      final RepositoryManagerV3Client client,
      final Map<DefaultComponent, List<ArtifactInfo>> deployables,
      final String tag)
      throws MojoExecutionException
  {
    log.info(String.format("Uploading %d components", deployables.size()));
    File target = getWorkDirectoryRoot();
    ArtifactRepository stagingRepository = createFileRepository(target);

    for (Entry<DefaultComponent, List<ArtifactInfo>> entry : deployables.entrySet()) {

      List<InputStream> streams = new ArrayList<>();
      DefaultComponent component = entry.getKey();
      try {
        for (ArtifactInfo info : entry.getValue()) {
          addArtifactToComponent(target, stagingRepository, streams, component, info);
        }
        client.upload(repository, component, tag);
      }
      catch (RepositoryManagerException ex) {
        if (log.isDebugEnabled()) {
          log.warn("Exception uploading component", ex);
        }
        else {
          log.warn(String.format("Exception uploading component: %s", ex.getLocalizedMessage()));
        }
        throw (new MojoExecutionException(ex));
      }
      finally {
        for (InputStream stream : streams) {
          try {
            stream.close();
          }
          catch (IOException ex) {
            // Ignore as nothing we can do
          }
        }
      }
    }
  }

  private ArtifactRepository createFileRepository(final File target) throws MojoExecutionException {
    if (!target.exists() || (!target.canWrite() || !target.isDirectory())) {
      throw new MojoExecutionException(
          "Upload failed: staging directory points to an existing file but is not a directory or is not writable!");
    }

    try {
      String url = target.getCanonicalFile().toURI().toURL().toExternalForm();
      return artifactRepositoryFactory.createDeploymentArtifactRepository("local-deployment", url,
          artifactRepositoryLayout, true);
    }
    catch (IOException e) {
      throw new MojoExecutionException(
          "Upload failed: staging directory path cannot be converted to canonical one!", e);
    }
  }

  private void addArtifactToComponent(
      final File target,
      final ArtifactRepository stagingRepository,
      final List<InputStream> streams,
      final DefaultComponent component,
      final ArtifactInfo info)
      throws MojoExecutionException
  {

    final String groupId = info.getGroup();
    final String artifactId = info.getArtifactId();
    final String artifactType = info.getPackaging();
    final DefaultArtifact defaultArtifact =
        new DefaultArtifact(groupId, artifactId,
            VersionRange.createFromVersion(info.getVersion()), null, artifactType,
            info.getClassifier(), new FakeArtifactHandler(artifactType, info.getExtension()));
    log.info(String.format("Artifact: %s", defaultArtifact));

    File assetFile = new File(target, stagingRepository.pathOf(defaultArtifact));
    defaultArtifact.setFile(assetFile);

    if (info.getPomFileName() != null) {
      final File associatedPomFile = new File(assetFile.getParentFile(), info.getPomFileName());
      final ProjectArtifactMetadata pom = new ProjectArtifactMetadata(defaultArtifact, associatedPomFile);
      defaultArtifact.addMetadata(pom);
      if ("maven-plugin".equals(defaultArtifact.getType())) {
        final GroupRepositoryMetadata groupMetadata = new GroupRepositoryMetadata(groupId);
        groupMetadata.addPluginMapping(info.getPluginPrefix(), artifactId, artifactId);
        defaultArtifact.addMetadata(groupMetadata);
      }
    }

    if (assetFile.exists()) {
      String assetName = assetFile.getName();
      processAsset(component, defaultArtifact, assetName, streams);
    }
    else {
      log.warn(String.format("Skipping asset as file not found: %s", assetFile));
    }
  }


  private void processAsset(
      final DefaultComponent component,
      final Artifact deployableArtifact,
      final String assetName,
      final List<InputStream> streams)
      throws MojoExecutionException
  {
    try {
      FileInputStream stream = new FileInputStream(deployableArtifact.getFile());
      streams.add(stream);
      DefaultAsset asset = new DefaultAsset(assetName, stream);

      // NEXUS-22246 - just like the maven class DefaultArtifactDeployer use the ArtifactHandler#getExtension()
      asset.addAttribute("extension", deployableArtifact.getArtifactHandler().getExtension());

      if (deployableArtifact.getClassifier() != null) {
        asset.addAttribute("classifier", deployableArtifact.getClassifier());
      }
      component.addAsset(asset);
    }
    catch (IOException ex) {
      if (log.isDebugEnabled()) {
        log.warn(String.format("Exception uploading asset %s", assetName), ex);
      }
      else {
        log.warn(String.format("Exception uploading asset %s: %s", assetName, ex.getLocalizedMessage()));
      }
      throw (new MojoExecutionException(ex));
    }
  }

  public static class ArtifactInfoKey
  {
    private final String groupId;

    private final String artifactId;

    private final String baseVersion;

    ArtifactInfoKey(final ArtifactInfo info) {
      this.groupId = info.getGroup();
      this.artifactId = info.getArtifactId();
      this.baseVersion = info.getVersion();
    }

    public String getGroupId() { return groupId; }

    public String getArtifactId() { return artifactId; }

    public String getBaseVersion() { return baseVersion; }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ArtifactInfoKey that = (ArtifactInfoKey) o;
      return groupId.equals(that.groupId) && artifactId.equals(that.artifactId) && baseVersion.equals(that.baseVersion);
    }

    @Override
    public int hashCode() {
      return Objects.hash(groupId, artifactId, baseVersion);
    }
  }

  public static class FakeArtifactHandler
      extends DefaultArtifactHandler
  {
    private final String extension;

    public FakeArtifactHandler(final String type, final String extension) {
      super(checkNotNull(type));
      this.extension = checkNotNull(extension);
    }

    @Override
    public String getExtension() {
      return extension;
    }
  }

  @VisibleForTesting
  void setRepository(final String repositoryName) { this.repository = repositoryName; }

  @VisibleForTesting
  void setArtifact(final Artifact artifact) {
    this.artifact = artifact;
  }

  @VisibleForTesting
  void setTag(final String tag) {
    this.tag = tag;
  }

  @VisibleForTesting
  void setTagGenerator(final TagGenerator tagGenerator) {
    this.tagGenerator = tagGenerator;
  }

  @VisibleForTesting
  void setArtifactRepositoryFactory(final ArtifactRepositoryFactory artifactRepositoryFactory) {
    this.artifactRepositoryFactory = artifactRepositoryFactory;
  }

  @VisibleForTesting
  void setArtifactRepositoryLayout(final ArtifactRepositoryLayout artifactRepositoryLayout) {
    this.artifactRepositoryLayout = artifactRepositoryLayout;
  }
}
