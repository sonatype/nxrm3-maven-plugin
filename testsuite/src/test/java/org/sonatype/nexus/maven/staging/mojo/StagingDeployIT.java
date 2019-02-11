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

import org.sonatype.nexus.maven.staging.test.support.StagingMavenPluginITSupport;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static java.util.UUID.randomUUID;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.StringStartsWith.startsWith;

public class StagingDeployIT
    extends StagingMavenPluginITSupport
{
  private static final String GROUP_ID = "test.group";

  private static final String VERSION = "1.0.0";

  private static final String STAGING_DEPLOY = "nexus-staging:staging-deploy";

  private static final String DEPLOY = "deploy";

  private File propertiesFile;

  @Before
  public void setup() {
    propertiesFile = new File(testDir.getAbsolutePath() + "/target/nexus-staging/staging/staging.properties");
  }

  @Test
  public void stagingDeploy() throws Exception {
    assertStagingWithDeployGoal(STAGING_DEPLOY);
  }

  @Test
  public void deploy() throws Exception {
    assertStagingWithDeployGoal(DEPLOY);
  }

  @Test
  public void failIfOffline() {
    String artifactId = randomUUID().toString();
    String tag = randomUUID().toString();

    createProject(RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION);

    List<String> goals = new ArrayList<>();

    goals.add("install");
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

    assertStagingWithDeployGoal(STAGING_DEPLOY, tag);

    assertThat(getTagFromPropertiesFile(), equalTo(tag));

    forceDelete(propertiesFile);

    assertStagingWithDeployGoal(STAGING_DEPLOY, tag);

    assertThat(getTagFromPropertiesFile(), equalTo(tag));
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
  public void deployWithoutSpecifyingTagUsesGeneratedTag() throws Exception {
    String artifactId = randomUUID().toString();

    createProject(RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION);

    List<String> goals = new ArrayList<>();

    goals.add("install");
    goals.add(STAGING_DEPLOY);

    verifier.setDebug(true);

    verifier.executeGoals(goals);

    String generatedTag = getTagFromPropertiesFile();
    assertThat(generatedTag, startsWith(artifactId + "-" + VERSION + "-"));

    verifyComponent(RELEASE_REPOSITORY, GROUP_ID, artifactId, VERSION, generatedTag);
  }

  private void assertStagingWithDeployGoal(final String deployGoal) throws Exception {
    assertStagingWithDeployGoal(deployGoal, randomUUID().toString());
  }

  private void assertStagingWithDeployGoal(final String deployGoal, final String tag) throws Exception {
    List<String> goals = new ArrayList<>();

    goals.add("install");
    goals.add("javadoc:jar");
    goals.add(deployGoal);
    
    assertStagingWithDeployGoal(goals, tag);
  }

  private void assertStagingWithDeployGoal(final List<String> goals, final String tag) throws Exception {
    String groupId = GROUP_ID;
    String artifactId = randomUUID().toString();
    String version = VERSION;

    createProject(RELEASE_REPOSITORY, groupId, artifactId, version);

    verifier.setDebug(true);

    verifier.addCliOption("-Dtag=" + tag);

    verifier.executeGoals(goals);

    verifyComponent(RELEASE_REPOSITORY, groupId, artifactId, version, tag);
  }

  private String getTagFromPropertiesFile() throws Exception {
    Properties properties = new Properties();
    properties.load(new FileInputStream(propertiesFile));
    return properties.getProperty("staging.tag");
  }
}
