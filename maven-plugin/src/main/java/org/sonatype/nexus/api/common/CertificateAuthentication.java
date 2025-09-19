/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.nexus.api.common;

import java.security.KeyStore;

public class CertificateAuthentication
{
  private final KeyStore keyStore;

  private final char[] keyPassword;

  public CertificateAuthentication(final KeyStore keyStore, final char[] keyPassword) {
    this.keyStore = keyStore;
    this.keyPassword = keyPassword;
  }

  public KeyStore getKeyStore() {
    return keyStore;
  }

  public char[] getKeyPassword() {
    return keyPassword;
  }
}
