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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.maven.staging.test.support.StagingMavenPluginITSupport;

import org.apache.maven.it.VerificationException;
import org.junit.Assert;
import org.junit.Test;

import static java.util.UUID.randomUUID;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.StringContains.containsString;

public class StagingMoveIT
    extends StagingMavenPluginITSupport
{
  private static final String GROUP_ID = "test.group";

  private static final String VERSION = "1.0.0";

  private static final String STAGING_DEPLOY = "nexus-staging:staging-deploy";

  private static final String STAGING_MOVE = "nexus-staging:move";

  private static final String INSTALL = "install";

  @Test
  public void failIfOffline() throws Exception {
    String tag = randomUUID().toString();

    initialiseVerifier(projectDir);

    deployAndTag(tag);

    List<String> goals = new ArrayList<>();

    goals.add(STAGING_MOVE);

    verifier.setDebug(true);

    verifier.addCliOption("-Dtag=" + tag);
    verifier.addCliOption("-o");

    try {
      verifier.executeGoals(goals);
      Assert.fail("Expected LifecycleExecutionException");
    }
    catch (Exception e) {
      assertThat(e.getMessage(),
          containsString("Goal requires online mode for execution but Maven is currently offline"));
    }
  }

  //Need to do these with and without the property in the pom.xml
  //    verifier.addCliOption("-Dtag=" + tag);

  @Test
  public void moveUsingTagFromPropertiesFile() throws Exception {
    String tag = randomUUID().toString();

    initialiseVerifier(projectDir);

    deployAndTag(tag);

    List<String> goals = new ArrayList<>();

    goals.add(STAGING_MOVE);

    verifier.setDebug(true);

    verifier.addCliOption("-Dtag=" + tag);
    verifier.addCliOption("-DsourceRepository="+);
    verifier.addCliOption("-DtargetRepository="+);

    //now attempt to move using the tag in the properties file
    //specify the targetRepository
    //specify the sourceRepository
  }

  @Test
  public void moveUsingUserDefinedTag() throws Exception {
    String tag = randomUUID().toString();

    assertStagingWithDeployGoal(STAGING_DEPLOY, tag);

    //now attempt to move using the tag in the properties file
    //specify the targetRepository
    //specify the sourceRepository
    //
  }

  @Test
  public void attemptMoveWithoutTargetRepository() throws Exception {
    String tag = randomUUID().toString();

    assertStagingWithDeployGoal(STAGING_DEPLOY, tag);

    //without the targetRepository
    //specify the sourceRepository
    //specify the tag
  }

  @Test
  public void attemptMoveWithTargetRepository() throws Exception {
    String tag = randomUUID().toString();

    assertStagingWithDeployGoal(STAGING_DEPLOY, tag);

    //specify the targetRepository
    //specify the sourceRepository
    //specify the tag
  }

  @Test
  public void attemptMoveWithoutSourceRepository() throws Exception {
    String tag = randomUUID().toString();

    assertStagingWithDeployGoal(STAGING_DEPLOY, tag);

    //specify the targetRepository
    //without the sourceRepository should use repository from pom
    //specify the tag
  }

  @Test
  public void attemptMoveWithSourceRepository() throws Exception {
    String tag = randomUUID().toString();

    assertStagingWithDeployGoal(STAGING_DEPLOY, tag);

    //specify the targetRepository
    //specifying the sourceRepository
    //specify the tag
  }

  @Test
  public void storeTagInPropertiesForNewAndExistingTag() throws Exception {
    String tag = randomUUID().toString();

    assertStagingWithDeployGoal(STAGING_DEPLOY, tag);

    File propertiesFile = new File(projectDir.getAbsolutePath() + "/target/nexus-staging/staging/staging.properties");

    assertThat(readFileToString(propertiesFile), containsString("staging.tag=" + tag));

    forceDelete(propertiesFile);

    assertStagingWithDeployGoal(STAGING_DEPLOY, tag);

    assertThat(readFileToString(propertiesFile), containsString("staging.tag=" + tag));
  }

  private void verifyNoComponentPresent(final String artifactId) throws Exception {
    Map<String, String> searchQuery = getSearchQuery(RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION);
    assertThat(componentSearch(searchQuery).items, hasSize(0));
  }

  private void assertStagingWithDeployGoal(final String deployGoal, final String tag) throws Exception {
    List<String> goals = new ArrayList<>();

    goals.add(INSTALL);
    goals.add("javadoc:jar");
    goals.add(deployGoal);

    assertStagingWithDeployGoal(goals, tag);
  }

  private void assertStagingWithDeployGoal(final List<String> goals,
                                           final String tag) throws Exception
  {
    initialiseVerifier(projectDir);

    assertStagingWithDeployGoal(goals, tag, JAR_PACKAGING);
  }

  private void assertStagingWithDeployGoal(final List<String> goals,
                                           final String tag,
                                           final String packaging) throws Exception
  {
    String groupId = GROUP_ID;
    String artifactId = randomUUID().toString();
    String version = VERSION;

    createProject(projectDir, RELEASE_REPOSITORY, groupId, artifactId, version);

    deployAndVerify(goals, tag, groupId, artifactId, version);
  }

  private void deployAndTag(final String tag) throws Exception {
    initialiseVerifier(projectDir);
    String artifactId = randomUUID().toString();

    createProject(projectDir, RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION);

    List<String> goals = new ArrayList<>();

    goals.add(INSTALL);
    goals.add(STAGING_DEPLOY);

    deployAndVerify(goals, tag, GROUP_ID, artifactId, VERSION);
  }

  private void deployAndVerify(final List<String> goals,
                               final String tag,
                               final String groupId,
                               final String artifactId,
                               final String version) throws VerificationException
  {
    verifier.setDebug(true);

    verifier.addCliOption("-Dtag=" + tag);

    verifier.executeGoals(goals);

    verifyComponent(RELEASE_REPOSITORY, groupId, artifactId, version, tag);
  }
}
