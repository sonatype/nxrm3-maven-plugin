/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
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
