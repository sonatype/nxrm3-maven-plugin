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

import static java.util.Objects.requireNonNull;

/**
 * This class is an abstraction of authentication credentials. Internally passwords are stored
 * as <tt>char[]</tt> to avoid logging any sensitive information as well as allowing the objects to be garbage
 * collected properly.
 */
public class Authentication
{
  private final String username;

  private final char[] password;

  /**
   * Both <tt>username</tt> and <tt>password</tt> are required.
   *
   * @param username the username.
   * @param password the password.
   *
   * @throws NullPointerException if either <tt>username</tt> or <tt>password</tt> not provided.
   */
  public Authentication(final String username, final char[] password) {
    requireNonNull(username, "Username is required.");
    requireNonNull(password, "Password is required.");

    this.username = username;
    this.password = password;
  }

  /**
   * Both <tt>username</tt> and <tt>password</tt> are required.
   *
   * @param username the username.
   * @param password the password.
   *
   * @throws NullPointerException if either <tt>username</tt> or <tt>password</tt> not provided.
   */
  public Authentication(final String username, final String password) {
    requireNonNull(username, "Username is required.");
    requireNonNull(password, "Password is required.");

    this.username = username;
    this.password = password.toCharArray();
  }

  /**
   * @return the username.
   */
  public String getUsername() {
    return username;
  }

  /**
   * @return the password.
   */
  public char[] getPassword() {
    return password;
  }
}
