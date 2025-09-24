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
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.sonatype.nexus.api.common.ServerConfig;
import org.sonatype.nexus.api.exception.RepositoryManagerException;
import org.sonatype.nexus.api.repository.v3.Component;
import org.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client;
import org.sonatype.nexus.api.repository.v3.Tag;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.nio.file.Files.createTempDirectory;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StagingUploadMojoTest
    extends AbstractMojoTestCase
{
  private static final String USERNAME = "username";

  private static final String PASSWORD = "password";

  private static final String TAG = "tag";

  private static final String GENERATED_TAG = "generatedTag";

  private static final String VERSION = "1.0.0";

  private static final String ARTIFACT_ID = "artifactid";

  private static final String GROUP_ID = "groupid";

  private static final String CLASSIFIER = "classifier";

  private static final String EXTENSION = "extension";

  @Mock
  private MavenSession session;

  @Mock
  private Settings settings;

  private Artifact artifact;

  @Mock
  private TagGenerator tagGenerator;

  @Mock
  private Nxrm3ClientFactory clientFactory;

  @Mock
  private RepositoryManagerV3Client client;

  private Path tempDirectory;

  private Properties userProperties;

  private StagingUploadMojo underTest;

  @Before
  public void setup() throws Exception {
    super.setUp();
    tempDirectory = createTempDirectory("test");
    artifact = new DefaultArtifact(GROUP_ID, ARTIFACT_ID, VERSION, null, EXTENSION, CLASSIFIER,
        new DefaultArtifactHandler());
    setupMockBehaviour();
    underTest = lookupMojo();
  }

  @After
  public void tearDown() throws Exception {

    // Clean up staging.properties if it exists
    File propsFile = new File(getBasedir(), "staging/staging.properties");
    if (propsFile.exists()) {
      propsFile.delete();
    }
    forceDelete(tempDirectory.toFile());
    super.tearDown();
  }

  @Test
  public void getWorkingDirectory() {
    File workDirectoryRoot = underTest.getWorkDirectoryRoot();

    String expected = tempDirectory + File.separator + "target" + File.separator + "nexus-staging";
    assertThat(workDirectoryRoot.getAbsolutePath(), is(equalTo(expected)));
  }

  @Test
  public void getAltWorkingDirectoryWhenConfigured() {
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
  public void createTagWhenSetAndDoesNotExist() throws Exception {
    when(client.getTag(TAG)).thenReturn(Optional.empty());

    underTest.execute();

    verify(client).createTag(TAG);
  }

  @Test
  public void generateTagWhenTagNotSet() throws Exception {
    underTest.setTag(null);
    when(client.getTag(GENERATED_TAG)).thenReturn(Optional.empty());

    underTest.execute();

    verify(client).createTag(GENERATED_TAG);
    assertThat(userProperties.getProperty("tag"), equalTo(GENERATED_TAG));
  }

  @Test
  public void skipCreateTagWhenTagExists() throws Exception {
    underTest.execute();

    verify(client, never()).createTag(TAG);
  }

  @Test
  public void saveTagToPropertiesFile() throws Exception {
    underTest.execute();

    File directory = underTest.getWorkDirectoryRoot();

    String props = readFileToString(new File(directory, "staging/staging.properties"));

    assertThat(props, containsString("staging.tag=" + TAG));
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwIllegalArgumentExceptionWhenIncorrectServerId() throws Exception {
    when(settings.getServer(anyString())).thenReturn(null);

    underTest.execute();
  }

  @Test
  public void getServerConfiguration() {
    ServerConfig config = underTest.getServerConfiguration(session);

    assertThat(config, is(notNullValue()));
  }

  @Test
  public void readIndexFile() {
    underTest.setStagingIndexFilename("example.index");
    underTest.setAltStagingDirectory(new File(getBasedir(), "src/test/resources/"));
    List<ArtifactInfo> artifacts = underTest.readStoredArtifactsFromIndex();
    assertThat(artifacts.size(), is(2));
  }

  @Test
  public void uploadArtifacts() throws MojoExecutionException, MojoFailureException, RepositoryManagerException {
    underTest.setStagingIndexFilename("example.index");
    underTest.setAltStagingDirectory(new File(getBasedir(), "src/test/resources/"));
    underTest.execute();

    ArgumentCaptor<Component> componentArgumentCaptor = ArgumentCaptor.forClass(Component.class);
    verify(client).upload(any(), componentArgumentCaptor.capture(), eq(TAG));
    assertThat(componentArgumentCaptor.getValue().getAssets().size(), is(2));
  }

  private StagingUploadMojo lookupMojo() throws Exception {
    File testPom = getPom();
    StagingUploadMojo mojo = (StagingUploadMojo) lookupMojo("upload", testPom);

    mojo.setMavenSession(session);
    mojo.setArtifact(artifact);
    mojo.setTag(TAG);
    mojo.setTagGenerator(tagGenerator);
    mojo.setClientFactory(clientFactory);
    return mojo;
  }

  private File getPom() {
    return new File(getBasedir(), "src/test/resources/example-pom.xml");
  }

  private void setupMockBehaviour() throws Exception {
    Server server = new Server();
    server.setUsername(USERNAME);
    server.setPassword(PASSWORD);

    userProperties = new Properties();

    when(session.getExecutionRootDirectory()).thenReturn(tempDirectory.toString());

    when(session.getSettings()).thenReturn(settings);

    when(session.getUserProperties()).thenReturn(userProperties);

    when(settings.getServer(anyString())).thenReturn(server);

    when(tagGenerator.generate(ARTIFACT_ID, VERSION)).thenReturn(GENERATED_TAG);

    when(clientFactory.build(any())).thenReturn(client);

    when(client.getTag(TAG)).thenReturn(Optional.of(new Tag(TAG)));
  }
}
