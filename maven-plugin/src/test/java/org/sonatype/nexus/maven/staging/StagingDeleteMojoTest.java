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

import static java.nio.file.Files.createTempDirectory;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sonatype.nexus.api.common.ServerConfig;
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client;

@RunWith(MockitoJUnitRunner.class)
public class StagingDeleteMojoTest
    extends AbstractMojoTestCase
{
  private static final String USERNAME = "username";

  private static final String PASSWORD = "password";

  private static final String TAG = "tag";

  @Mock
  private MavenSession session;

  @Mock
  private Settings settings;

  @Mock
  private Nxrm3ClientFactory clientFactory;

  @Mock
  private RepositoryManagerV3Client client;

  private Server server;

  private StagingDeleteMojo underTest;

  private Path tempDirectory;

  @Before
  public void setup() throws Exception {
    super.setUp();

    tempDirectory = createTempDirectory("test");

    underTest = lookupMojo();

    setupMockBehaviour();
  }

  @After
  public void tearDown() throws Exception {
    File propsFile = new File(getBasedir(), "staging/staging.properties");
    if (propsFile.exists()) {
      propsFile.delete();
    }
    forceDelete(tempDirectory.toFile());
  }

  @Test
  public void getWorkingDirectory() throws Exception {
    File workDirectoryRoot = underTest.getWorkDirectoryRoot();

    String expected = tempDirectory + File.separator + "target" + File.separator + "nexus-staging";

    assertThat(workDirectoryRoot.getAbsolutePath(), is(equalTo(expected)));
  }

  @Test
  public void getAltWorkingDirectoryWhenConfigured() throws Exception {
    File dir = new File("test");

    underTest.setAltStagingDirectory(dir);

    assertThat(underTest.getWorkDirectoryRoot(), is(equalTo(dir)));
  }

  @Test(expected = MojoFailureException.class)
  public void executeFailsWhenOffline() throws Exception {
    underTest.setOffline(true);

    underTest.execute();
  }

  @Test
  public void testDefaultValueWhenTagIsNull() throws Exception {
    createPropertiesFile(TAG, true);

    underTest.setTag(null);

    underTest.execute();

    verify(client).delete(eq(TAG));
  }

  @Test
  public void testDefaultValueWhenTagIsEmpty() throws Exception {
    createPropertiesFile(TAG, true);

    underTest.setTag("");

    underTest.execute();

    verify(client).delete(eq(TAG));
  }

  @Test
  public void testDelete() throws Exception {
    underTest.execute();

    verify(client).delete(eq(TAG));
  }

  @Test(expected = MojoFailureException.class)
  public void mojoFailureExceptionOnDeleteFail() throws Exception {
    doThrow(new RuntimeException()).when(client).delete(anyString());

    underTest.execute();
  }

  @Test(expected = MojoExecutionException.class)
  public void mojoFailureExceptionOnPropertiesFileNotFound() throws Exception {
    underTest.setTag(null);

    underTest.execute();
  }

  @Test(expected = MojoExecutionException.class)
  public void mojoFailureExceptionOnTagNotPresentInPropertiesFile() throws Exception {
    createPropertiesFile("", true);

    underTest.setTag("");

    underTest.execute();
  }

  @Test(expected = MojoExecutionException.class)
  public void mojoFailureExceptionOnTagPropertyNotDefinedInPropertiesFile() throws Exception {
    createPropertiesFile(TAG, false);

    underTest.setTag("");

    underTest.execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwIllegalArgumentExceptionWhenIncorrectServerId() throws Exception {
    createPropertiesFile(TAG, true);

    when(settings.getServer(anyString())).thenReturn(null);

    underTest.execute();
  }

  @Test
  public void getServerConfiguration() throws Exception {
    ServerConfig config = underTest.getServerConfiguration(session);

    assertThat(config, is(notNullValue()));
  }

  private void createPropertiesFile(String tag, boolean addTag) throws IOException {
    File stagingPropertiesFile = new File(underTest.getWorkDirectoryRoot(), "staging/staging.properties");
    stagingPropertiesFile.getParentFile().mkdirs();
    final Properties stagingProperties = new Properties();
    if (addTag) {
      stagingProperties.put("staging.tag", tag);
    }
    try (OutputStream out = new FileOutputStream(stagingPropertiesFile)) {
      stagingProperties.store(out, "NXRM3 Maven staging plugin");
    }
  }

  private StagingDeleteMojo lookupMojo() throws Exception {
    File testPom = getPom();
    StagingDeleteMojo mojo = (StagingDeleteMojo) lookupMojo("staging-delete", testPom);

    mojo.setMavenSession(session);
    mojo.setTag(TAG);
    mojo.setClientFactory(clientFactory);

    return mojo;
  }

  private File getPom() {
    return new File(getBasedir(), "src/test/resources/example-pom-without-repo-config.xml");
  }

  private void setupMockBehaviour() throws Exception {
    server = new Server();
    server.setUsername(USERNAME);
    server.setPassword(PASSWORD);

    when(session.getExecutionRootDirectory()).thenReturn(tempDirectory.toString());

    when(session.getSettings()).thenReturn(settings);

    when(settings.getServer(anyString())).thenReturn(server);

    when(clientFactory.build(any())).thenReturn(client);

  }

}
