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

import java.net.URI;

import com.sonatype.nexus.api.common.Authentication;
import com.sonatype.nexus.api.common.ServerConfig;
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client;
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3ClientBuilder;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class StagingMojo
    extends AbstractMojo
{
  @Parameter(property = "serverId", required = true)
  private String serverId;

  @Parameter(property = "nexusUrl", required = true)
  private String nexusUrl;

  //These come from the settings.xml file...need to see how that works...
  //for now I'm making them parameters in the pom.xml file
  @Parameter(property = "username", required = true)
  private String username;

  @Parameter(property = "password", required = true)
  private String password;



  protected  final String servicesBase = "/service/rest/beta";

  protected String getNexusUrl() {
    return nexusUrl;
  }

  protected String getServerId() {
    return serverId;
  }

  protected ServerConfig getServerConfiguration() {
    return new ServerConfig(URI.create(nexusUrl), new Authentication("admin", "admin123"));
  }

  protected RepositoryManagerV3Client getClient() {
    final ServerConfig serverConfig = getServerConfiguration();
    RepositoryManagerV3Client client = RepositoryManagerV3ClientBuilder.create().withServerConfig(serverConfig).build();
    return client;
  }
}
