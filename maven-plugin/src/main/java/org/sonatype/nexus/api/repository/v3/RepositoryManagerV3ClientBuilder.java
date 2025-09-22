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

import org.sonatype.nexus.api.common.ProxyConfig;
import org.sonatype.nexus.api.common.ServerConfig;
import org.sonatype.nexus.api.repository.AbstractRepositoryManagerClientBuilder;
import org.sonatype.nexus.api.repository.v3.impl.DefaultNexusRepositoryV3Client;

import org.apache.http.client.HttpClient;

import static java.util.Objects.requireNonNull;

/**
 * This class is used to build a {@link RepositoryManagerV3Client}. A {@link ServerConfig} containing the configuration
 * for connecting to the Nexus Repository Manager is required using the
 * {@link RepositoryManagerV3ClientBuilder#withServerConfig(ServerConfig)} builder method. You may optionally provide
 * proxy connection information using the {@link RepositoryManagerV3ClientBuilder#withProxyConfig(ProxyConfig)} method
 * as well.
 *
 * If you have very unique connection requirements that are not covered by this library, you may also provide an
 * {@link HttpClient} using the {@link RepositoryManagerV3ClientBuilder#withHttpClient(HttpClient)} builder method in
 * lieu
 * of providing the {@link ServerConfig} containing connection information.
 *
 * Once you are done configuring the builder, you must invoke the {@link RepositoryManagerV3ClientBuilder#build()}
 * method
 * to create the actual {@link RepositoryManagerV3Client}. The builder will throw a {@link NullPointerException} if you
 * do not provide a {@link ServerConfig} or an {@link HttpClient} to connect to the Nexus Repository Manager.
 *
 * @since 3.0
 */
public class RepositoryManagerV3ClientBuilder
    extends AbstractRepositoryManagerClientBuilder<RepositoryManagerV3ClientBuilder>
{
  // this is protected to prevent creating directly
  private RepositoryManagerV3ClientBuilder() {
  }

  /**
   * This is the entry point to this builder. You must first <tt>create</tt> the builder then continue to configure
   * it using the fluent configuration methods.
   *
   * @return a fresh builder to configure.
   */
  public static RepositoryManagerV3ClientBuilder create() {
    return new RepositoryManagerV3ClientBuilder();
  }

  @Override
  protected RepositoryManagerV3ClientBuilder getThis() {
    return this;
  }

  /**
   * @return a {@link RepositoryManagerV3Client} to interact with a Nexus Repository Manager 3.x server
   */
  public RepositoryManagerV3Client build() {
    if (httpClient == null) {
      requireNonNull(serverConfig, "Nexus server configuration is required");
      httpClient = buildHttpClient(serverConfig, proxyConfig);
    }

    return new DefaultNexusRepositoryV3Client(serverConfig, httpClient);
  }
}
