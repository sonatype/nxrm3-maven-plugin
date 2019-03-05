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

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client;

import com.google.common.collect.ImmutableMap;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static java.nio.file.Files.createTempDirectory;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StagingMoveMojoTest
    extends AbstractMojoTestCase
{
  private static final String USERNAME = "username";

  private static final String PASSWORD = "password";

  private static final String TAG = "tag";

  private static final String SOURCE_REPOSITORY = "maven-test";

  private static final String TARGET_REPOSITORY = "maven-releases";

  private static final String EMPTY = "";

  private static final String DEFAULT_REPOSITORY = "maven-releases";

  @Mock
  private MavenSession session;

  @Mock
  private Settings settings;

  @Mock
  private Nxrm3ClientFactory clientFactory;

  @Mock
  private RepositoryManagerV3Client client;

  private Server server;

  private StagingMoveMojo underTest;

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
    forceDelete(tempDirectory.toFile());
  }

  @Test(expected = MojoFailureException.class)
  public void testFailsWhenOffline() throws Exception {
    underTest.setOffline(true);

    underTest.execute();
  }

  @Test(expected = MojoFailureException.class)
  public void testFailureWhenTargetRepositoryIsNull() throws Exception {
    underTest.setTargetRepository(null);

    underTest.execute();
  }

  @Test
  public void testUseDefaultRepositoryWhenSourceRepositoryNotProvided() throws Exception {
    underTest.setSourceRepository(null);

    assertThat(underTest.getSourceRepository(), equalTo(DEFAULT_REPOSITORY));
  }

  @Test(expected = MojoFailureException.class)
  public void testFailWhenTagIsNull() throws Exception {
    //No tag is defined by the user and no staging.properties file exists
    underTest.setTag(null);

    underTest.execute();
  }

  @Test
  public void testGetTagFromPropertiesFileWhenNotProvided() throws Exception {
    //No tag is defined by the user but the tag is in the staging.properties
    setupPropertiesFile("staging.tag=" + TAG);
    underTest.setTag(null);

    assertThat(underTest.getTagForMoving(), equalTo(TAG));
  }

  @Test(expected = MojoFailureException.class)
  public void testFailWhenTagIsNotFound() throws Exception {
    //No tag is defined by the user and no tag is in the staging.properties
    setupPropertiesFile(EMPTY);
    underTest.setTag(null);

    underTest.execute();
  }

  @Test
  public void testGetSearchCriteria() throws Exception {
    Map<String, String> expectedCriteria = ImmutableMap.of("repository", "foo", "tag", "bah");
    assertThat(underTest.createSearchCriteria("foo", "bah"), equalTo(expectedCriteria));
  }

  @Test
  public void testMove() throws Exception {
    underTest.execute();

    Map<String, String> searchCriteria = ImmutableMap.of("repository", SOURCE_REPOSITORY, "tag", TAG);
    verify(client).move(eq(TARGET_REPOSITORY), eq(searchCriteria));
  }

  private StagingMoveMojo lookupMojo() throws Exception {
    File testPom = getPom();
    StagingMoveMojo mojo = (StagingMoveMojo) lookupMojo("move", testPom);

    mojo.setMavenSession(session);
    mojo.setTag(TAG);
    mojo.setSourceRepository(SOURCE_REPOSITORY);
    mojo.setTargetRepository(TARGET_REPOSITORY);
    mojo.setClientFactory(clientFactory);
    return mojo;
  }

  private File getPom() {
    return new File(getBasedir(), "src/test/resources/example-pom.xml");
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

  private void setupPropertiesFile(final String propertyString) throws Exception {
    File directory = underTest.getWorkDirectoryRoot();
    writeStringToFile(new File(directory, "staging/staging.properties"), propertyString);
  }
}
