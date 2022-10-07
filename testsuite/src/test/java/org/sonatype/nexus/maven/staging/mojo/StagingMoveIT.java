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
package org.sonatype.nexus.maven.staging.mojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.sonatype.nexus.maven.staging.test.support.StagingMavenPluginITSupport;

import com.google.common.collect.ImmutableList;
import org.apache.maven.it.VerificationException;
import org.junit.Test;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;

public class StagingMoveIT
    extends StagingMavenPluginITSupport
{
  private static final String GROUP_ID = "test.group";

  private static final String VERSION = "1.0.0";

  private static final String STAGING_DEPLOY = "nxrm3:staging-deploy";

  private static final String STAGING_MOVE = "nxrm3:staging-move";

  private static final String INSTALL = "install";

  private static final List<String> MOVE_GOALS = ImmutableList.of("nxrm3:staging-move");

  @Test
  public void failIfOffline() throws Exception {
    String tag = randomUUID().toString();
    String artifactId = randomUUID().toString();

    initialiseVerifier(projectDir);

    assertStagingWithDeployGoal(STAGING_DEPLOY, tag, artifactId);

    List<String> goals = new ArrayList<>();
    goals.add(STAGING_MOVE);

    verifier.setDebug(true);
    verifier.addCliOption("-Dtag=" + tag);
    verifier.addCliOption("-o");

    try {
      verifier.executeGoals(goals);
      fail("Expected LifecycleExecutionException");
    }
    catch (Exception e) {
      assertThat(e.getMessage(),
          containsString("Goal requires online mode for execution but Maven is currently offline"));
    }
  }

  @Test
  public void moveUsingUserDefinedMoveProperties() throws Exception {
    String tag = randomUUID().toString();
    String artifactId = randomUUID().toString();

    prepareForMove(tag, artifactId);

    verifier.setDebug(true);
    verifier.addCliOption("-Dtag=" + tag);
    verifier.addCliOption("-DsourceRepository=" + RELEASE_REPOSITORY);
    verifier.addCliOption("-DdestinationRepository=" + testName.getMethodName());

    verifier.executeGoals(MOVE_GOALS);

    verifyNoComponentPresent(artifactId);

    verifyComponent(testName.getMethodName(), GROUP_ID, artifactId, VERSION, tag);
  }

  @Test
  public void moveForMultiModuleUsingUserDefinedMoveProperties() throws Exception {
    initialiseVerifier(multiModuleProjectDir);

    String tag = randomUUID().toString();

    List<String> goals = new ArrayList<>();

    goals.add(INSTALL);
    goals.add(STAGING_DEPLOY);

    String groupId = GROUP_ID;
    String artifactId = randomUUID().toString();
    String version = VERSION;

    createProject(multiModuleProjectDir, RELEASE_REPOSITORY, groupId, artifactId, version);
    createProject(project1Dir, RELEASE_REPOSITORY, groupId, artifactId, version);
    createProject(project2Dir, RELEASE_REPOSITORY, groupId, artifactId, version);

    verifier.setDebug(true);
    verifier.addCliOption("-Dtag=" + tag);
    verifier.executeGoals(goals);

    verifyComponent(RELEASE_REPOSITORY, groupId, artifactId, version, tag);
    verifyComponent(RELEASE_REPOSITORY, groupId, artifactId + "-module1", version, tag);
    verifyComponent(RELEASE_REPOSITORY, groupId, artifactId + "-module2", version, tag);

    //perform move
    createTargetRepo(testName.getMethodName());

    verifier.addCliOption("-Dtag=" + tag);
    verifier.addCliOption("-DsourceRepository=" + RELEASE_REPOSITORY);
    verifier.addCliOption("-DdestinationRepository=" + testName.getMethodName());
    verifier.executeGoals(MOVE_GOALS);

    verifyComponent(testName.getMethodName(), groupId, artifactId, version, tag);
    verifyComponent(testName.getMethodName(), groupId, artifactId + "-module1", version, tag);
    verifyComponent(testName.getMethodName(), groupId, artifactId + "-module2", version, tag);
  }

  @Test
  public void moveUsingStagingPropertiesFileProvidedTag() throws Exception {
    String artifactId = randomUUID().toString();

    prepareForMove(randomUUID().toString(), artifactId);

    Properties properties = loadStagingProperties();
    String tag = properties.getProperty("staging.tag");
    assertThat(tag, is(notNullValue()));

    verifier.setDebug(true);
    verifier.addCliOption("-DsourceRepository=" + RELEASE_REPOSITORY);
    verifier.addCliOption("-DdestinationRepository=" + testName.getMethodName());
    verifier.executeGoals(MOVE_GOALS);

    verifyNoComponentPresent(artifactId);

    verifyComponent(testName.getMethodName(), GROUP_ID, artifactId, VERSION, tag);
  }

  @Test
  public void moveUsingPomConfiguration() throws Exception {
    String destinationRepository = "maven-test-hosted";
    String tag = randomUUID().toString();
    String artifactId = randomUUID().toString();

    //Perform initial deploy and move using default and user defined properties
    createProject(projectDir, RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION);
    createTargetRepo(testName.getMethodName());
    assertStagingWithDeployGoalPomProperties(STAGING_DEPLOY, artifactId, tag);

    verifier.addCliOption("-Dtag=" + tag);
    verifier.addCliOption("-DsourceRepository=" + RELEASE_REPOSITORY);
    verifier.addCliOption("-DdestinationRepository=" + testName.getMethodName());
    verifier.executeGoals(MOVE_GOALS);

    verifyNoComponentPresent(artifactId);
    verifyComponent(testName.getMethodName(), GROUP_ID, artifactId, VERSION, tag);


    //Perform a move based on properties in the pom
    createProject(projectDir, RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION, testName.getMethodName(),
        destinationRepository);
    createTargetRepo(destinationRepository);

    verifier.setDebug(true);
    verifier.addCliOption("-Dtag=" + tag);
    verifier.executeGoals(MOVE_GOALS);

    verifyNoComponentPresent(artifactId);
    verifyComponent(destinationRepository, GROUP_ID, artifactId, VERSION, tag);
  }

  @Test
  public void moveUsingDefaultSourceRepositoryFromPom() throws Exception {
    String tag = randomUUID().toString();
    String artifactId = randomUUID().toString();

    prepareForMove(tag, artifactId);

    verifier.setDebug(true);

    verifier.addCliOption("-Dtag=" + tag);
    verifier.addCliOption("-DdestinationRepository=" + testName.getMethodName());
    verifier.executeGoals(MOVE_GOALS);

    verifyNoComponentPresent(artifactId);

    verifyComponent(testName.getMethodName(), GROUP_ID, artifactId, VERSION, tag);
  }

  @Test
  public void moveErrorWhenTagNotFound() throws Exception {
    String artifactId = randomUUID().toString();

    prepareForMove(randomUUID().toString(), artifactId);

    try {
      verifier.addCliOption("-Dtag=" + "bogusTag");
      verifier.addCliOption("-DsourceRepository=" + RELEASE_REPOSITORY);
      verifier.addCliOption("-DdestinationRepository=" + testName.getMethodName());
      verifier.executeGoals(MOVE_GOALS);
      fail("Expected LifecycleExecutionException");
    }
    catch (Exception e) {
      assertThat(e.getMessage(), containsString("Reason: No components found"));
    }
  }

  @Test
  public void moveErrorWhenIncompatibleSourceAndTargetRepository() throws Exception {
    String tag = randomUUID().toString();
    String artifactId = randomUUID().toString();

    prepareForMove(tag, artifactId);

    try {
      verifier.addCliOption("-Dtag=" + tag);
      verifier.addCliOption("-DsourceRepository=" + RELEASE_REPOSITORY);
      verifier.addCliOption("-DdestinationRepository=" + "nuget-hosted");
      verifier.executeGoals(MOVE_GOALS);

      fail("Expected LifecycleExecutionException");
    }
    catch (Exception e) {
      assertThat(e.getMessage(), containsString("Reason: Source and destination repository formats do not match"));
    }
  }

  private void prepareForMove(final String tag, final String artifactId) throws Exception  {
    assertStagingWithDeployGoal(STAGING_DEPLOY, artifactId, tag);

    createTargetRepo(testName.getMethodName());
  }

  private void assertStagingWithDeployGoal(final String deployGoal,
                                           final String artifactId,
                                           final String tag) throws Exception
  {
    List<String> goals = new ArrayList<>();
    goals.add(INSTALL);
    goals.add("javadoc:jar");
    goals.add(deployGoal);

    initialiseVerifier(projectDir);

    String groupId = GROUP_ID;
    String version = VERSION;

    createProject(projectDir, RELEASE_REPOSITORY, groupId, artifactId, version);

    deployAndVerify(goals, tag, groupId, artifactId, version);
  }

  private void assertStagingWithDeployGoalPomProperties(final String deployGoal,
                                                        final String artifactId,
                                                        final String tag) throws Exception
  {
    List<String> goals = new ArrayList<>();
    goals.add(INSTALL);
    goals.add("javadoc:jar");
    goals.add(deployGoal);

    initialiseVerifier(projectDir);

    String groupId = GROUP_ID;
    String version = VERSION;

    deployAndVerify(goals, tag, groupId, artifactId, version);
  }

  private void deployAndVerify(final List<String> goals,
                               final String tag,
                               final String groupId,
                               final String artifactId,
                               final String version) throws VerificationException
  {
    verifier.addCliOption("-Dtag=" + tag);
    verifier.executeGoals(goals);

    verifyComponent(RELEASE_REPOSITORY, groupId, artifactId, version, tag);
  }

  private void verifyNoComponentPresent(final String artifactId) throws Exception {
    Map<String, String> searchQuery = getSearchQuery(RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION);
    await().atMost(10, SECONDS).until(() -> componentSearch(searchQuery).items, hasSize(0));
  }

  private Properties loadStagingProperties() throws Exception {
    File propertiesFile = new File(projectDir.getAbsolutePath() + "/target/nexus-staging/staging/staging.properties");
    final Properties properties = new Properties();
    try (InputStream inputStream = Files.newInputStream(propertiesFile.toPath())) {
      properties.load(inputStream);
    }
    return properties;
  }
}
