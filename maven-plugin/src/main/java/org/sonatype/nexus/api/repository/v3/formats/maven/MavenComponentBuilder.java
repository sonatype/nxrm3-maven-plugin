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
package org.sonatype.nexus.api.repository.v3.formats.maven;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.api.repository.v3.Component;
import org.sonatype.nexus.api.repository.v3.DefaultAsset;
import org.sonatype.nexus.api.repository.v3.DefaultComponent;

import static org.sonatype.nexus.api.common.ArgumentUtils.checkArgument;
import static org.sonatype.nexus.api.common.NexusStringUtils.isBlank;
import static org.sonatype.nexus.api.common.NexusStringUtils.isNotBlank;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.StandardOpenOption.READ;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * Builder class to simply constructing a Maven {@link Component} for upload to NXRM3
 *
 * @since 3.0
 */
public class MavenComponentBuilder
{
  private static final String POM = "pom";

  static final String POM_ASSET_EXTENSION = POM;

  static final String POM_ASSET_FILENAME = POM + ".xml";

  static final String JAR_ASSET_EXTENSION = "jar";

  static final String FORMAT = "maven2";

  static final String ATTR_GROUP_ID = "groupId";

  static final String ATTR_ARTIFACT_ID = "artifactId";

  static final String ATTR_VERSION = "version";

  static final String ATTR_PACKAGING = "packaging";

  static final String ATTR_GEN_POM = "generate-pom";

  static final String ASSET_ATTR_EXTENSION = "extension";

  static final String ASSET_ATTR_CLASSIFIER = "classifier";

  static final Map<String, String> EXTENSIONS_BY_PACKAGE = new HashMap<String, String>(15)
  {
    {
      put("ejb-client", "jar");
      put("ejb", "jar");
      put("rar", "jar");
      put("par", "jar");
      put("maven-plugin", "jar");
      put("maven-archetype", "jar");
      put("plexus-application", "jar");
      put("eclipse-plugin", "jar");
      put("eclipse-feature", "jar");
      put("eclipse-application", "zip");
      put("nexus-plugin", "jar");
      put("java-source", "jar");
      put("javadoc", "jar");
      put("test-jar", "jar");
      put("bundle", "jar");
    }
  };

  private String groupId;

  private String artifactId;

  private String version;

  private String packaging;

  private Collection<DefaultAsset> assets;

  public static MavenComponentBuilder create() {
    return new MavenComponentBuilder();
  }

  private MavenComponentBuilder() {
    assets = new ArrayList<>();
  }

  /**
   * Builds the {@link Component} based on the builder's configured properties
   *
   * @return a component that can be uploaded to NXRM3
   * @throws IllegalArgumentException if the component cannot be built due to an incomplete configuration
   */
  public Component build() {
    DefaultComponent component = new DefaultComponent(FORMAT);
    long numPoms = countPoms();

    if (numPoms > 1) {
      throw new IllegalStateException(
          "Cannot build Component: only 1 pom is allowed, but " + numPoms + " have been added");
    }

    if (numPoms == 0) {
      component.addAttribute(ATTR_GROUP_ID,
          checkArgument(groupId, isNotBlank(groupId), "Maven groupId is required when pom is not supplied"));
      component.addAttribute(ATTR_ARTIFACT_ID,
          checkArgument(artifactId, isNotBlank(artifactId), "Maven artifactId is required when pom is not supplied"));
      component.addAttribute(ATTR_VERSION,
          checkArgument(version, isNotBlank(version), "Maven version is required when pom is not supplied"));

      component.addAttribute(ATTR_GEN_POM, "true");
    }

    if (isNotBlank(packaging)) {
      component.addAttribute(ATTR_PACKAGING, packaging);
    }

    assets.forEach(a -> {
      if (isBlank(a.getAttribute(ASSET_ATTR_EXTENSION))) {
        String ext = isNotBlank(packaging) ? EXTENSIONS_BY_PACKAGE.getOrDefault(packaging, packaging) : null;
        a.addAttribute(ASSET_ATTR_EXTENSION,
            checkArgument(ext, isNotBlank(ext), "Asset extension was not specified for asset '" + a.getFilename()
                + "' and could not be determined by packaging"));
      }

      component.addAsset(a);
    });

    return component;
  }

  /**
   * @param groupId the groupId value for the maven component
   * @return {@code this} for method chaining
   * @see <a href="https://maven.apache.org/pom.html#Maven_Coordinates">Maven Coordinates</a>
   */
  public MavenComponentBuilder withGroupId(final String groupId) {
    this.groupId = groupId;
    return this;
  }

  /**
   * @param artifactId the artifactId value for the maven component
   * @return {@code this} for method chaining
   * @see <a href="https://maven.apache.org/pom.html#Maven_Coordinates">Maven Coordinates</a>
   */
  public MavenComponentBuilder withArtifactId(final String artifactId) {
    this.artifactId = artifactId;
    return this;
  }

  /**
   * @param version the version value for the maven component
   * @return {@code this} for method chaining
   * @see <a href="https://maven.apache.org/pom.html#Maven_Coordinates">Maven Coordinates</a>
   */
  public MavenComponentBuilder withVersion(final String version) {
    this.version = version;
    return this;
  }

  /**
   * @param packaging the artifact type for the component
   * @return {@code this} for method chaining
   * @see <a href="https://maven.apache.org/pom.html#Maven_Coordinates">Maven Coordinates</a>
   */
  public MavenComponentBuilder withPackaging(final String packaging) {
    this.packaging = packaging;
    return this;
  }

  /**
   * @param asset the {@link DefaultAsset} to be added to the component. The asset's extension will be determined by
   *          packaging ({@link #withPackaging(String)}) or the {@code asset}'s {@link DefaultAsset#getFilename()}
   * @return {@code this} for method chaining
   */
  public MavenComponentBuilder withAsset(DefaultAsset asset) {
    return withAsset(asset, null, null);
  }

  /**
   * @param asset the {@link DefaultAsset} to be added to the component.
   * @param extension the asset's extension, which if not specified will be determined by packaging
   *          ({@link #withPackaging(String)}) or the {@code asset}'s {@link DefaultAsset#getFilename()}
   * @return {@code this} for method chaining
   */
  public MavenComponentBuilder withAsset(DefaultAsset asset, String extension) {
    return withAsset(asset, extension, null);
  }

  /**
   * @param asset the {@link DefaultAsset} to be added to the component.
   * @param extension the asset's extension, which if not specified will be determined by packaging
   *          ({@link #withPackaging(String)}) or the {@code asset}'s {@link DefaultAsset#getFilename()}
   * @param classifier the asset's classifier (optional)
   * @return {@code this} for method chaining
   */
  public MavenComponentBuilder withAsset(DefaultAsset asset, String extension, String classifier) {
    if (isNotBlank(extension)) {
      asset.addAttribute(ASSET_ATTR_EXTENSION, extension);
    }

    if (isNotBlank(classifier)) {
      asset.addAttribute(ASSET_ATTR_CLASSIFIER, classifier);
    }

    assets.add(asset);
    return this;
  }

  /**
   * Convenience method for adding a pom file to the component
   *
   * @param pomFilePath the fully qualified file path of the pom
   * @return this
   * @throws IOException if the pom file cannot be read
   */
  public MavenComponentBuilder withPom(final String pomFilePath) throws IOException {
    return withPom(pomFilePath, null);
  }

  /**
   * Convenience method for adding a pom file to the component
   *
   * @param pomFilePath the fully qualified file path of the pom
   * @param classifier optional classifier
   * @return this
   * @throws IOException if the pom file cannot be read
   */
  public MavenComponentBuilder withPom(final String pomFilePath, final String classifier) throws IOException {
    Path pom = Paths.get(pomFilePath);
    checkArgument(exists(pom), "Pom file does not exist");
    return withPom(newInputStream(pom, READ), classifier);
  }

  /**
   * Convenience method for adding a pom file to the component
   *
   * @param pomStream {@link InputStream} for the pom
   * @return this
   */
  public MavenComponentBuilder withPom(final InputStream pomStream) {
    return withPom(pomStream, null);
  }

  /**
   * Convenience method for adding a pom file to the component
   *
   * @param pomStream {@link InputStream} for the pom
   * @param classifier optional classifier
   * @return this
   */
  public MavenComponentBuilder withPom(final InputStream pomStream, final String classifier) {
    requireNonNull(pomStream, "Pom input stream is required");
    DefaultAsset pomAsset = new DefaultAsset(POM_ASSET_FILENAME, pomStream);
    pomAsset.addAttribute(ASSET_ATTR_EXTENSION, POM_ASSET_EXTENSION);

    if (isNotBlank(classifier)) {
      pomAsset.addAttribute(ASSET_ATTR_CLASSIFIER, classifier);
    }

    assets.add(pomAsset);
    return this;
  }

  /**
   * Convenience method for adding a jar file to the component
   *
   * @param jarFilePath the fully qualified file path of the jar
   * @return this
   * @throws IOException if the jar file cannot be read
   */
  public MavenComponentBuilder withJar(String jarFilePath) throws IOException {
    return withJar(jarFilePath, null);
  }

  /**
   * Convenience method for adding a jar file to the component
   *
   * @param jarFilePath the fully qualified file path of the jar
   * @param classifier optional classifier
   * @return this
   * @throws IOException if the pom file cannot be read
   */
  public MavenComponentBuilder withJar(final String jarFilePath, final String classifier) throws IOException {
    Path jar = Paths.get(jarFilePath);
    checkArgument(exists(jar), "Jar file does not exist");
    return withJar(newInputStream(jar, READ), jar.getFileName().toString(), classifier);
  }

  /**
   * Convenience method for adding a jar file to the component
   *
   * @param jarStream {@link InputStream} for the jar
   * @return this
   */
  public MavenComponentBuilder withJar(final InputStream jarStream) {
    return withJar(jarStream, null, null);
  }

  /**
   * Convenience method for adding a jar file to the component
   *
   * @param jarStream {@link InputStream} for the jar
   * @param filename optional filename of the jar
   * @return this
   */
  public MavenComponentBuilder withJar(final InputStream jarStream, final String filename) {
    return withJar(jarStream, filename, null);
  }

  /**
   * Convenience method for adding a jar file to the component
   *
   * @param jarStream {@link InputStream} for the jar
   * @param filename optional filename of the jar
   * @param classifier optional classifier
   * @return this
   */
  public MavenComponentBuilder withJar(
      final InputStream jarStream,
      final String filename,
      final String classifier)
  {
    requireNonNull(jarStream, "Jar input stream is required");
    String jarFilename = ofNullable(filename).orElse("asset-jar." + JAR_ASSET_EXTENSION);
    DefaultAsset jarAsset = new DefaultAsset(jarFilename, jarStream);
    jarAsset.addAttribute(ASSET_ATTR_EXTENSION, JAR_ASSET_EXTENSION);

    if (isNotBlank(classifier)) {
      jarAsset.addAttribute(ASSET_ATTR_CLASSIFIER, classifier);
    }

    assets.add(jarAsset);
    return this;
  }

  private long countPoms() {
    return assets.stream().filter(a -> POM_ASSET_EXTENSION.equals(a.getAttribute(ASSET_ATTR_EXTENSION))).count();
  }
}
