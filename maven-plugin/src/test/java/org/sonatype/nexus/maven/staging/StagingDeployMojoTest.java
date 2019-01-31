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
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.sonatype.nexus.api.common.ServerConfig;
import com.sonatype.nexus.api.repository.v3.Asset;
import com.sonatype.nexus.api.repository.v3.Component;
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client;
import com.sonatype.nexus.api.repository.v3.Tag;

import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import com.google.common.collect.ImmutableList;
import org.apache.maven.artifact.Artifact;
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
import org.mockito.runners.MockitoJUnitRunner;

import static java.nio.file.Files.createTempDirectory;
import static java.util.Collections.emptyList;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StagingDeployMojoTest
    extends AbstractMojoTestCase
{

  private static final String USERNAME = "username";

  private static final String PASSWORD = "password";

  private static final String TAG = "tag";

  private static final String VERSION = "1.0.0";

  private static final String ARTIFACT_ID = "artifactid";

  private static final String GROUP_ID = "groupid";

  private static final String CLASSIFIER = "classifier";

  private static final String EXTENSION = "extension";

  private static final String REPOSITORY = "maven-releases";

  private static final String ARTIFACT_ID_KEY = "artifactId";

  private static final String GROUP_ID_KEY = "groupId";

  private static final String VERSION_KEY = "version";

  private static final String CLASSIFIER_KEY = "classifier";

  private static final String EXTENSION_KEY = "extension";

  @Mock
  private MavenSession session;

  @Mock
  private Settings settings;

  @Mock
  private SecDispatcher secDispatcher;

  @Mock
  private Artifact artifact, attachedArtifact;

  @Mock
  private Nxrm3ClientFactory clientFactory;

  @Mock
  private RepositoryManagerV3Client client;

  private Server server;

  private StagingDeployMojo underTest;

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

  @Test
  public void getWorkingDirectory() throws Exception {
    File workDirectoryRoot = underTest.getWorkDirectoryRoot();

    String expected = tempDirectory + "/target/nexus-staging";
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

  @Test(expected = IllegalArgumentException.class)
  public void executeFailsWhenSecDispatcherThrowsException() throws Exception {
    when(secDispatcher.decrypt(anyString())).thenThrow(new SecDispatcherException());

    underTest.setSecDispatcher(secDispatcher);

    underTest.execute();
  }

  @Test(expected = MojoExecutionException.class)
  public void failWhenTagNotSet() throws Exception {
    underTest.setTag(null);

    underTest.execute();
  }

  @Test
  public void createTagWhenSetAndDoesNotExist() throws Exception {
    when(client.getTag(TAG)).thenReturn(Optional.empty());

    underTest.execute();

    verify(client).createTag(TAG);
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

  @Test
  public void upload() throws Exception {
    underTest.execute();

    ArgumentCaptor<Component> componentArgumentCaptor = ArgumentCaptor.forClass(Component.class);

    verify(client).upload(eq(REPOSITORY), componentArgumentCaptor.capture(), eq(TAG));

    Component component = componentArgumentCaptor.getValue();

    Map<String, String> attributes = component.getAttributes();
    assertThat(attributes.get(ARTIFACT_ID_KEY), is(equalTo(ARTIFACT_ID)));
    assertThat(attributes.get(GROUP_ID_KEY), is(equalTo(GROUP_ID)));
    assertThat(attributes.get(VERSION_KEY), is(equalTo(VERSION)));

    Collection<Asset> assets = component.getAssets();

    assertThat(assets.size(), is(equalTo(2)));

    for (Asset asset : assets) {
      Map<String, String> assetAttributes = asset.getAttributes();

      assertThat(assetAttributes.get(EXTENSION_KEY), is(equalTo(EXTENSION)));
      assertThat(assetAttributes.get(CLASSIFIER_KEY), is(equalTo(CLASSIFIER)));
    }
  }

  @Test(expected = MojoFailureException.class)
  public void mojoFailureExceptionOnUploadFail() throws Exception {
    doThrow(new RuntimeException()).when(client).upload(any(), any(), any());

    underTest.execute();
  }

  @Test(expected = MojoExecutionException.class)
  public void exceptionWhenNoFilesAssignedToBuild() throws Exception {
    when(artifact.getFile()).thenReturn(null);
    underTest.setAttachedArtifacts(emptyList());

    underTest.execute();
  }

  @Test
  public void deployOnlyAttachedArtifactsAndPomWhenNoMainArtifact() throws Exception {
    when(artifact.getFile()).thenReturn(null);

    underTest.execute();

    ArgumentCaptor<Component> componentArgumentCaptor = ArgumentCaptor.forClass(Component.class);

    verify(client).upload(eq(REPOSITORY), componentArgumentCaptor.capture(), eq(TAG));

    Component component = componentArgumentCaptor.getValue();

    assertThat(component.getAssets().size(), is(equalTo(2)));

    Map<String, String> firstAssetAttributes = component.getAssets().iterator().next().getAttributes();
    assertThat(firstAssetAttributes.get(EXTENSION_KEY), is(equalTo("pom")));
    assertThat(firstAssetAttributes.size(), is(equalTo(1)));
  }

  @Test
  public void addPomToBuildWhenMainArtifactNotPom() throws Exception {
    underTest.setPackaging("jar");

    underTest.execute();

    verify(artifact).addMetadata(any());
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwIllegalArgumentExceptionWhenIncorrectServerId() throws Exception {
    when(settings.getServer(anyString())).thenReturn(null);

    underTest.execute();
  }

  @Test
  public void getServerConfiguration() throws Exception {
    ServerConfig config = underTest.getServerConfiguration(session);

    assertThat(config, is(notNullValue()));
  }

  private StagingDeployMojo lookupMojo() throws Exception {
    File testPom = getPom();
    StagingDeployMojo mojo = (StagingDeployMojo) lookupMojo("staging-deploy", testPom);

    mojo.setMavenSession(session);
    mojo.setArtifact(artifact);
    mojo.setTag(TAG);
    mojo.setAttachedArtifacts(ImmutableList.of(attachedArtifact));
    mojo.setClientFactory(clientFactory);
    mojo.setPomFile(testPom);

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

    mockArtifact(artifact);
    mockArtifact(attachedArtifact);

    when(clientFactory.build(any())).thenReturn(client);

    when(client.getTag(TAG)).thenReturn(Optional.of(new Tag(TAG)));
  }

  private void mockArtifact(final Artifact artifact) {
    when(artifact.getGroupId()).thenReturn(GROUP_ID);
    when(artifact.getArtifactId()).thenReturn(ARTIFACT_ID);
    when(artifact.getBaseVersion()).thenReturn(VERSION);
    when(artifact.getFile()).thenReturn(getPom());
    when(artifact.getType()).thenReturn(EXTENSION);
    when(artifact.getClassifier()).thenReturn(CLASSIFIER);
  }
}
