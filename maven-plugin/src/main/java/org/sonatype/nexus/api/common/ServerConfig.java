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

package org.sonatype.nexus.api.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.sonatype.nexus.api.common.CertificateAuthentication;

import static java.util.Objects.requireNonNull;

/**
 * This class is used to represent the <tt>address</tt> and optionally any <tt>authentication</tt> information
 * necessary to connect to an external server resource.
 */
public class ServerConfig
{
  private final URI address;

  private final Authentication authentication;

  private final Optional<CertificateAuthentication> certificateAuthentication;

  /**
   * The {@link URI} representing the address is required.
   *
   * @param address the URI of the external resource.
   *
   * @throws URISyntaxException if <tt>address</tt> is not valid.
   * @throws NullPointerException if <tt>address</tt> not provided.
   */
  public ServerConfig(final URI address) throws URISyntaxException {
    this(address, (Authentication) null);
  }

  /**
   * The {@link URI} representing the address is required.
   *
   * @param address the URI of the external resource.
   * @param authentication the authentication details.
   *
   * @throws NullPointerException if <tt>address</tt> not provided.
   */
  public ServerConfig(final URI address, final Authentication authentication) {
    requireNonNull(address, "Address must not be null");
    this.address = address.getPath().endsWith("/") ? address : address.resolve(address.getPath() + "/").normalize();
    this.authentication = authentication;
    this.certificateAuthentication = Optional.empty();
  }

  /**
   * The {@link URI} representing the address is required.
   *
   * @param address the URI of the external resource.
   * @param certificateAuthentication the certificate authentication details.
   *
   * @throws NullPointerException if <tt>address</tt> not provided.
   */
  public ServerConfig(final URI address, final CertificateAuthentication certificateAuthentication) {
    requireNonNull(address, "Address must not be null");
    this.address = address.getPath().endsWith("/") ? address : address.resolve(address.getPath() + "/").normalize();
    this.certificateAuthentication = Optional.of(certificateAuthentication);
    this.authentication = null;
  }

  /**
   *
   * @return the URI of the external resource.
   */
  public URI getAddress() {
    return address.normalize();
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
   * @return returns true if certificate authentication is configured, otherwise false.
   */
  public boolean isCertificateAuthentication() {
    return certificateAuthentication.isPresent();
  }

  /**
   *
   * @return the X509 certificate authentication details.
   */
  public CertificateAuthentication getCertificateAuthentication() {
    if (!certificateAuthentication.isPresent()) {
      throw new IllegalStateException("Not configured for certificate authn");
    }
    return certificateAuthentication.get();
  }
}
