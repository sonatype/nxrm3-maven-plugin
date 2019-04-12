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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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
import static org.hamcrest.core.StringStartsWith.startsWith;

public class StagingDeployIT
    extends StagingMavenPluginITSupport
{
  private static final String GROUP_ID = "test.group";

  private static final String VERSION = "1.0.0";

  private static final String STAGING_DEPLOY = "nxrm3-staging:staging-deploy";

  private static final String DEPLOY = "deploy";

  private static final String INSTALL = "install";

  @Test
  public void stagingDeploy() throws Exception {
    assertStagingWithDeployGoal(STAGING_DEPLOY);
  }

  @Test
  public void deploy() throws Exception {
    assertStagingWithDeployGoal(DEPLOY);
  }

  @Test
  public void multiModuleDeploy() throws Exception {
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
  }

  @Test
  public void failIfOffline() throws Exception {
    initialiseVerifier(projectDir);

    String artifactId = randomUUID().toString();
    String tag = randomUUID().toString();

    createProject(projectDir, RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION);

    List<String> goals = new ArrayList<>();

    goals.add(INSTALL);
    goals.add(STAGING_DEPLOY);

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

  @Test
  public void useExistingTag() throws Exception {
    String tag = randomUUID().toString();

    assertStagingWithDeployGoal(STAGING_DEPLOY, tag);

    assertStagingWithDeployGoal(STAGING_DEPLOY, tag);
  }

  @Test
  public void storeTagInPropertiesForNewAndExistingTag() throws Exception {
    String tag = randomUUID().toString();

    File propertiesFile = getPropertiesFile(projectDir);

    assertStagingWithDeployGoal(STAGING_DEPLOY, tag);

    assertThat(readFileToString(propertiesFile), containsString("staging.tag=" + tag));

    forceDelete(propertiesFile);

    assertStagingWithDeployGoal(STAGING_DEPLOY, tag);

    assertThat(readFileToString(propertiesFile), containsString("staging.tag=" + tag));
  }

  @Test
  public void deployWithoutSpecifyingTagUsesGeneratedTag() throws Exception {
    initialiseVerifier(multiModuleProjectDir);

    String artifactId = randomUUID().toString();

    createProject(multiModuleProjectDir, RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION);
    createProject(project1Dir, RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION);
    createProject(project2Dir, RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION);

    List<String> goals = new ArrayList<>();

    goals.add(INSTALL);
    goals.add(STAGING_DEPLOY);

    verifier.setDebug(true);

    verifier.executeGoals(goals);

    Properties properties = new Properties();
    properties.load(new FileInputStream(getPropertiesFile(multiModuleProjectDir)));
    String generatedTag = properties.getProperty("staging.tag");

    assertThat(generatedTag, startsWith(artifactId + "-" + VERSION + "-"));

    verifyComponent(RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION, generatedTag);
    verifyComponent(RELEASE_REPOSITORY, GROUP_ID, artifactId + "-module1", VERSION, generatedTag);
    verifyComponent(RELEASE_REPOSITORY, GROUP_ID, artifactId + "-module2", VERSION, generatedTag);
  }

  @Test
  public void deployWithoutMainArtifact() throws Exception {
    String tag = randomUUID().toString();

    List<String> goals = new ArrayList<>();

    goals.add("javadoc:jar");
    goals.add(STAGING_DEPLOY);

    assertStagingWithDeployGoal(goals, tag);
  }

  @Test
  public void stagingDeployPomProject() throws Exception {
    initialiseVerifier(projectDir);

    List<String> goals = new ArrayList<>();

    goals.add(INSTALL);
    goals.add(STAGING_DEPLOY);

    assertStagingWithDeployGoal(goals, randomUUID().toString(), POM_PACKAGING);
  }

  @Test
  public void deployIfSkipNotSet() throws Exception {
    initialiseVerifier(projectDir);
    String artifactId = randomUUID().toString();
    String tag = randomUUID().toString();

    createProject(projectDir, RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION);

    List<String> goals = new ArrayList<>();
    goals.add(INSTALL);
    goals.add(STAGING_DEPLOY);

    deployAndVerify(goals, tag, GROUP_ID, artifactId, VERSION);
  }

  @Test
  public void doNothingIfSkipTrue() throws Exception {
    initialiseVerifier(projectDir);
    String artifactId = randomUUID().toString();
    String tag = randomUUID().toString();

    createProject(projectDir, RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION, true);

    List<String> goals = new ArrayList<>();
    goals.add(INSTALL);
    goals.add(STAGING_DEPLOY);

    verifier.setDebug(true);

    verifier.addCliOption("-Dtag=" + tag);

    verifier.executeGoals(goals);

    verifyNoComponentPresent(artifactId);
  }

  @Test
  public void deployIfSkipFalse() throws Exception {
    initialiseVerifier(projectDir);
    String artifactId = randomUUID().toString();
    String tag = randomUUID().toString();

    createProject(projectDir, RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION, false);

    List<String> goals = new ArrayList<>();

    goals.add(INSTALL);
    goals.add(STAGING_DEPLOY);

    deployAndVerify(goals, tag, GROUP_ID, artifactId, VERSION);
  }

  @Test
  public void deployIfCliSkipClear() throws Exception {
    initialiseVerifier(projectDir);
    String artifactId = randomUUID().toString();
    String tag = randomUUID().toString();

    createProject(projectDir, RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION);

    List<String> goals = new ArrayList<>();

    goals.add(INSTALL);
    goals.add(STAGING_DEPLOY);

    verifier.addCliOption("-DskipNexusStagingDeployMojo=false");

    deployAndVerify(goals, tag, GROUP_ID, artifactId, VERSION);
  }

  @Test
  public void doNothingIfCliSkipSet() throws Exception {
    initialiseVerifier(projectDir);
    String artifactId = randomUUID().toString();
    String tag = randomUUID().toString();

    createProject(projectDir, RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION);

    List<String> goals = new ArrayList<>();

    goals.add(INSTALL);
    goals.add(STAGING_DEPLOY);

    verifier.addCliOption("-DskipNexusStagingDeployMojo=true");

    verifier.setDebug(true);

    verifier.addCliOption("-Dtag=" + tag);

    verifier.executeGoals(goals);

    verifyNoComponentPresent(artifactId);
  }

  private void verifyNoComponentPresent(final String artifactId) throws Exception {
    Map<String, String> searchQuery = getSearchQuery(RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION);
    assertThat(componentSearch(searchQuery).items, hasSize(0));
  }

  private void assertStagingWithDeployGoal(final String deployGoal) throws Exception {
    assertStagingWithDeployGoal(deployGoal, randomUUID().toString());
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

  private File getPropertiesFile(final File projectDir) {
    return new File(projectDir.getAbsolutePath() + "/target/nexus-staging/staging/staging.properties");
  }
}
