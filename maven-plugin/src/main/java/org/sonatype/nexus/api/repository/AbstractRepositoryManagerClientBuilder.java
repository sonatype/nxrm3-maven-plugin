/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.nexus.api.repository;

import org.sonatype.nexus.api.common.ProxyConfig;
import org.sonatype.nexus.api.common.ServerConfig;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;

/**
 * Base class for constructing a builder that will create a client for interacting with Nexus Repository Manager
 *
 * @param <T> self-referencing subclass type to allow implementations to return themselves
 * @since 3.0
 */
public abstract class AbstractRepositoryManagerClientBuilder<T extends AbstractRepositoryManagerClientBuilder<T>>
{
  protected ServerConfig serverConfig;

  protected ProxyConfig proxyConfig;

  protected HttpClient httpClient;

  protected String userAgent;

  protected abstract T getThis();

  /**
   * @param serverConfig the server configuration for the Nexus Repository Manager.
   * @return the builder.
   */
  public T withServerConfig(final ServerConfig serverConfig) {
    this.serverConfig = serverConfig;
    return getThis();
  }

  /**
   * @param proxyConfig the proxy configuration for the Nexus Repository Manager.
   * @return the builder.
   */
  public T withProxyConfig(final ProxyConfig proxyConfig) {
    this.proxyConfig = proxyConfig;
    return getThis();
  }

  /**
   * This builder method is <em>optional</em> and should only be used when using the default {@link HttpClient} is not
   * able to connect to the Nexus Repository Manager.
   *
   * @param httpClient the {@link HttpClient} to use when connecting to the Nexus Repository Manager.
   * @return the builder.
   */
  public T withHttpClient(final HttpClient httpClient) {
    this.httpClient = httpClient;
    return getThis();
  }

  public T withUserAgent(String userAgent) {
    this.userAgent = userAgent;
    return getThis();
  }

  protected HttpClient buildHttpClient(final ServerConfig serverConfig, final ProxyConfig proxyConfig) {
    HttpClientBuilder httpClientBuilder = HttpClients.custom();
    httpClientBuilder.setUserAgent(userAgent != null ? userAgent : "nxrm3-maven-plugin-client");

    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();

    if (serverConfig.getAuthentication() != null) {
      addCredentialsToProviderWithConfig(credentialsProvider, serverConfig);
    }

    if (proxyConfig != null) {
      HttpHost proxy = new HttpHost(proxyConfig.getHost(), proxyConfig.getPort());
      // TODO how remplase ProxyExcludeHostsRoutePlanner
     // DefaultProxyRoutePlanner routePlanner = new ProxyExcludeHostsRoutePlanner(proxy, proxyConfig.getNoProxyHosts());
      //httpClientBuilder.setRoutePlanner(routePlanner);

      if (proxyConfig.getAuthentication() != null) {
        addCredentialsToProviderWithConfig(credentialsProvider, proxyConfig);
      }
    }

    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
    httpClientBuilder.useSystemProperties();
    return httpClientBuilder.build();
  }

  private void addCredentialsToProviderWithConfig(
      final BasicCredentialsProvider credentialsProvider,
      final ServerConfig config)
  {
    String username = config.getAuthentication().getUsername();
    String password = new String(config.getAuthentication().getPassword());
    addCredentialsToProviderWithConfig(credentialsProvider, username, password, config.getAddress().getHost(),
        config.getAddress().getPort());
  }

  private void addCredentialsToProviderWithConfig(
      final BasicCredentialsProvider credentialsProvider,
      final ProxyConfig config)
  {
    String username = config.getAuthentication().getUsername();
    String password = new String(config.getAuthentication().getPassword());
    addCredentialsToProviderWithConfig(credentialsProvider, username, password, config.getHost(), config.getPort());
  }

  private void addCredentialsToProviderWithConfig(
      final BasicCredentialsProvider credentialsProvider,
      final String username,
      final String password,
      final String host,
      final int port)
  {
    UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
    AuthScope authscope = new AuthScope(host, port);
    credentialsProvider.setCredentials(authscope, credentials);
  }
}
