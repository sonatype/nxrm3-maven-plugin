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
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3ClientBuilder;

import org.sonatype.maven.mojo.execution.MojoExecution;
import org.sonatype.maven.mojo.settings.MavenSettings;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;

/**
 * Parent for all staging MOJOs (goals)
 * 
 * @since 1.0.0
 */
public abstract class StagingMojo
    extends AbstractMojo
{
  private static final String TAG_ID = "staging.tag";
  
  private static final String STAGING_PROPERTIES_FILENAME = "staging.properties";
  
  private static final String NEXUS_STAGING_OUTPUT_DIRECTORY = "nexus-staging";

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession mavenSession;
  
  @Parameter(property = "serverId", required = true)
  private String serverId;

  @Parameter(property = "nexusUrl", required = true)
  private String nexusUrl;

  /**
   * Specifies an alternative staging directory to which the project artifacts should be "locally staged". By
   * default, staging directory will be looked for under {@code $}{{@code project.build.directory} {@code
   * /nexus-staging}
   * folder of the first encountered module that has this Mojo defined for execution (Warning: this means, if top
   * level POM is an aggregator, it will NOT be in the top level!).
   */
  @Parameter(property = "altStagingDirectory")
  private File altStagingDirectory;

  @Parameter(defaultValue = "${plugin.groupId}", readonly = true, required = true)
  private String pluginGroupId;

  @Parameter(defaultValue = "${plugin.artifactId}", readonly = true, required = true)
  private String pluginArtifactId;

  @Component
  private SecDispatcher secDispatcher;

  protected ServerConfig getServerConfiguration(final MavenSession mavenSession) {
    final Server server = MavenSettings.selectServer(mavenSession.getSettings(), serverId);
    try {
      if (server != null) {
        Server decryptedServer = MavenSettings.decrypt(secDispatcher, server);

        return new ServerConfig(URI.create(nexusUrl),
            new Authentication(decryptedServer.getUsername(), decryptedServer.getPassword()));
      }
      else {
        throw new IllegalArgumentException("Server with ID \"" + serverId + "\" not found!");
      }
    }
    catch (SecDispatcherException e) {
      throw new IllegalArgumentException("Cannot decipher credentials to be used with Nexus!", e);
    }
  }

  protected RepositoryManagerV3Client getClient(final MavenSession mavenSession) {
    final ServerConfig serverConfig = getServerConfiguration(mavenSession);
    RepositoryManagerV3Client client = RepositoryManagerV3ClientBuilder.create().withServerConfig(serverConfig).build();
    return client;
  }

  /**
   * Returns the working directory root, that is either set explicitly by user in plugin configuration
   * (see {@link #altStagingDirectory} parameter), or it's location is calculated taking as base the first project in
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

  /**
   * Returns the first project in reactor that has this plugin defined.
   */
  protected MavenProject getFirstProjectWithThisPluginDefined() {
    return MojoExecution.getFirstProjectWithMojoInExecution(mavenSession, pluginGroupId, pluginArtifactId, null);
  }

  /**
   * Returns the staging directory root, that is either set explicitly by user in plugin configuration
   * (see {@link #altStagingDirectory} parameter), or it's location is calculated taking as base the first project in
   * this reactor that will/was executing this plugin.
   */
  protected File getStagingDirectoryRoot() {
    return new File(getWorkDirectoryRoot(), "staging");
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
}
