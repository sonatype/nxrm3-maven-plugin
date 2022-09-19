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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.sonatype.nexus.api.common.Authentication;
import com.sonatype.nexus.api.common.ServerConfig;
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client;

import org.sonatype.maven.mojo.execution.MojoExecution;
import org.sonatype.maven.mojo.settings.MavenSettings;

import com.google.common.annotations.VisibleForTesting;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;

/**
 * Parent for all staging MOJOs (goals)
 * 
 * @since 1.0.0
 */
public abstract class StagingMojo
    extends AbstractMojo
{
  static final String TAG_ID = "staging.tag";
  
  private static final String STAGING_PROPERTIES_FILENAME = "staging.properties";
  
  private static final String NEXUS_STAGING_OUTPUT_DIRECTORY = "nexus-staging";

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession mavenSession;
  
  @Parameter(property = "serverId")
  private String serverId;

  @Parameter(property = "nexusUrl")
  private String nexusUrl;

  /**
   * Specifies an alternative staging directory to which the project artifacts should be "locally staged". By
   * default, staging directory will be looked for under {@code $}{{@code project.build.directory} {@code
   * /nexus-staging} folder of the first encountered module that has this Mojo defined for execution (Warning: this 
   * means, if top level POM is an aggregator, it will NOT be in the top level!).
   */
  @Parameter(property = "altStagingDirectory")
  private File altStagingDirectory;

  @Parameter(defaultValue = "${plugin.groupId}", readonly = true, required = true)
  private String pluginGroupId;

  @Parameter(defaultValue = "${plugin.artifactId}", readonly = true, required = true)
  private String pluginArtifactId;

  @Parameter(defaultValue = "${settings.offline}", readonly = true, required = true)
  private boolean offline;

  @Component
  private SettingsDecrypter settingsDecrypter;

  private Nxrm3ClientFactory clientFactory = new Nxrm3ClientFactory();

  protected ServerConfig getServerConfiguration(final MavenSession mavenSession) {
    final Server server = MavenSettings.selectServer(mavenSession.getSettings(), serverId);
    if (server != null) {
      SettingsDecryptionResult result = settingsDecrypter.decrypt(new DefaultSettingsDecryptionRequest(server));

      Server decryptedServer = result.getServer();

      return new ServerConfig(URI.create(nexusUrl),
          new Authentication(decryptedServer.getUsername(), decryptedServer.getPassword()));
    }
    else {
      throw new IllegalArgumentException("Server with ID \"" + serverId + "\" not found!");
    }
  }

  /**
   * Returns the working directory root, that is either set explicitly by the user in the plugin configuration
   * (see {@link #altStagingDirectory} parameter), or its location is calculated taking as base the first project in
   * this reactor that will/was executing this plugin.
   */
  protected File getWorkDirectoryRoot() {
    if (altStagingDirectory != null) {
      return altStagingDirectory;
    }
    else {
      final MavenProject firstWithThisMojo = getFirstProjectWithThisPluginDefined();
      if (firstWithThisMojo != null) {
        final File firstWithThisMojoBuildDir;
        if (firstWithThisMojo.getBuild() != null && firstWithThisMojo.getBuild().getDirectory() != null) {
          firstWithThisMojoBuildDir =
              new File(firstWithThisMojo.getBuild().getDirectory()).getAbsoluteFile();
        }
        else {
          firstWithThisMojoBuildDir = new File(firstWithThisMojo.getBasedir().getAbsoluteFile(), "target");
        }
        return new File(firstWithThisMojoBuildDir, NEXUS_STAGING_OUTPUT_DIRECTORY);
      }
      else {
        return new File(getMavenSession().getExecutionRootDirectory() + "/target/" + NEXUS_STAGING_OUTPUT_DIRECTORY);
      }
    }
  }

  protected void storeTagInPropertiesFile(final String tag) {
    Map<String, String> properties = new HashMap<>();

    properties.put(TAG_ID, tag);

    saveStagingProperties(properties);
  }

  protected void saveStagingProperties(final Map<String, String> properties) {
    final Properties stagingProperties = new Properties();

    for (Entry<String, String> entry : properties.entrySet()) {
      stagingProperties.put(entry.getKey(), entry.getValue());
    }

    final File stagingPropertiesFile = getStagingPropertiesFile();

    if (!(stagingPropertiesFile.getParentFile().isDirectory() || stagingPropertiesFile.getParentFile().mkdirs())) {
        getLog().warn("Unable to create directory for stagings properties file");
    }

    try (OutputStream out = new FileOutputStream(stagingPropertiesFile)) {
      getLog().info(String.format("Saving staging information to %s", stagingPropertiesFile.getAbsolutePath()));

      stagingProperties.store(out, "NXRM3 Maven staging plugin");
    }
    catch (IOException e) {
      getLog().error(e);
    }
  }

  /**
   * Returns the first project in reactor that has this plugin defined.
   */
  protected MavenProject getFirstProjectWithThisPluginDefined() {
    return MojoExecution.getFirstProjectWithMojoInExecution(mavenSession, pluginGroupId, pluginArtifactId, null);
  }

  /**
   * Returns the staging directory root, that is either set explicitly by the user in the plugin configuration
   * (see {@link #altStagingDirectory} parameter), or its location is calculated taking as base the first project in
   * this reactor that will/was executing this plugin.
   */
  protected File getStagingDirectoryRoot() {
    return new File(getWorkDirectoryRoot(), "staging");
  }

  /**
   * Returns an instance of the {@link RepositoryManagerV3Client}
   */
  protected RepositoryManagerV3Client getRepositoryManagerV3Client() {
    return getClientFactory().build(getServerConfiguration(getMavenSession()));
  }

  protected String getNexusUrl() {
    return nexusUrl;
  }

  protected String getServerId() {
    return serverId;
  }

  protected MavenSession getMavenSession() {
    return mavenSession;
  }

  protected File getStagingPropertiesFile() {
    return new File(getStagingDirectoryRoot(), STAGING_PROPERTIES_FILENAME);
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
  void setMavenSession(final MavenSession session) {
    this.mavenSession = session;
  }

  protected Nxrm3ClientFactory getClientFactory() {
    return clientFactory;
  }

  @VisibleForTesting
  void setClientFactory(final Nxrm3ClientFactory clientFactory) {
    this.clientFactory = clientFactory;
  }

  @VisibleForTesting
  void setAltStagingDirectory(final File altStagingDirectory) {
    this.altStagingDirectory = altStagingDirectory;
  }

  @VisibleForTesting
  void setOffline(final boolean offline) {
    this.offline = offline;
  }


}
