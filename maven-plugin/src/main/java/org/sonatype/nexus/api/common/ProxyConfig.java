/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.nexus.api.common;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * This class is used to represent the <tt>location</tt> and optionally any <tt>authentication</tt> information
 * necessary to connect to a proxy server.
 */
public class ProxyConfig
{
  private final String host;

  private final int port;

  private final Authentication authentication;

  private final List<String> noProxyHosts;

  /**
   * The {@link String} representing the location is required.
   *
   * @param host the host of the proxy
   * @param port the port of the proxy
   *
   * @throws NullPointerException if <tt>location</tt> not provided.
   */
  public ProxyConfig(final String host, final int port) {
    this(host, port, null);
  }

  /**
   * The {@link String} representing the location is required.
   *
   * @param host the host of the proxy
   * @param port the port of the proxy
   * @param authentication the authentication details.
   *
   * @throws NullPointerException if <tt>location</tt> not provided.
   */
  public ProxyConfig(final String host, final int port, final Authentication authentication) {
    this(host, port, authentication, null);
  }

  /**
   * @param host the host of the proxy
   * @param port the port of the proxy
   * @param authentication the authentication details
   * @param noProxyHosts the host to exclude from proxy configuration
   *
   * @throws NullPointerException if <tt>location</tt> not provided.
   */
  public ProxyConfig(
      final String host,
      final int port,
      final Authentication authentication,
      final List<String> noProxyHosts)
  {
    requireNonNull(host, "Host must not be null");
    this.host = host;
    this.port = port;
    this.authentication = authentication;
    this.noProxyHosts = noProxyHosts;
  }

  /**
   *
   * @return the host of the proxy.
   */
  public String getHost() {
    return host;
  }

  /**
   *
   * @return the port of the proxy.
   */
  public int getPort() {
    return port;
  }

  /**
   *
   * @return the authentication details.
   */
  public Authentication getAuthentication() {
    return authentication;
  }

  /**
   *
   * @return the list of pattern with the excluded hosts
   */
  public List<String> getNoProxyHosts() {
    return noProxyHosts;
  }
}
