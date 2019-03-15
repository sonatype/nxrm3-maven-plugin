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
package org.sonatype.nexus.maven.staging.test.support;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import org.sonatype.sisu.filetasks.FileTaskBuilder;

import org.apache.maven.it.Verifier;
import org.junit.Before;

import static org.apache.maven.it.util.ResourceExtractor.simpleExtractResources;
import static org.sonatype.sisu.filetasks.builder.FileRef.file;

public abstract class StagingMavenPluginITSupport
    extends NxrmITSupport
{
  private static final String DIRECTORY = "/maven3-project";

  private static final String MULTI_MODULE_DIRECTORY = "/multi-module-maven3-project";

  private static final String PROJECT_1_DIRECTORY = MULTI_MODULE_DIRECTORY + "/Proj1";

  private static final String PROJECT_2_DIRECTORY = MULTI_MODULE_DIRECTORY + "/Proj2";

  private static final String GROUP_ID = "org.sonatype.plugins";

  private static final String ARTIFACT_ID = "nexus-staging-example-project";

  private static final String VERSION = "1.0.0";

  protected static final String POM_PACKAGING = "pom";

  protected static final String JAR_PACKAGING = "jar";

  protected static final String RELEASE_REPOSITORY = "maven-releases";

  @Inject
  private FileTaskBuilder fileTaskBuilder;

  protected File projectDir;

  protected File multiModuleProjectDir;

  protected File project1Dir;

  protected File project2Dir;

  protected Verifier verifier;

  @Before
  public void setupItSupport() throws Exception {
    projectDir = simpleExtractResources(StagingMavenPluginITSupport.class, DIRECTORY);

    multiModuleProjectDir = simpleExtractResources(StagingMavenPluginITSupport.class, MULTI_MODULE_DIRECTORY);

    project1Dir = simpleExtractResources(StagingMavenPluginITSupport.class, PROJECT_1_DIRECTORY);

    project2Dir = simpleExtractResources(StagingMavenPluginITSupport.class, PROJECT_2_DIRECTORY);
  }

  protected void createProject(final File dir,
                               final String repository,
                               final String groupId,
                               final String artifactId,
                               final String version) {
    createProject(dir, repository, groupId, artifactId, version, JAR_PACKAGING);
  }

  protected void createProject(final File dir,
                               final String repository,
                               final String groupId,
                               final String artifactId,
                               final String version,
                               final String packaging)
  {
    final File pom = new File(dir, "pom.xml");
    final File rawPom = new File(dir, "raw-pom.xml");

    final Properties properties = getDefaultProperties(repository, groupId, artifactId, version, packaging);

    fileTaskBuilder.copy().file(file(rawPom)).filterUsing(properties).to().file(file(pom)).run();
  }

  protected void createProject(final File dir,
                               final String repository,
                               final String groupId,
                               final String artifactId,
                               final String version,
                               final boolean skip)
  {
    final File pom = new File(dir, "pom.xml");
    final File rawPom = new File(dir, "raw-pom-with-skip.xml");

    final Properties properties = getDefaultProperties(repository, groupId, artifactId, version, JAR_PACKAGING);
    properties.setProperty("nexus.skipNexusStagingDeployMojo", skip ? "true" : "false");

    fileTaskBuilder.copy().file(file(rawPom)).filterUsing(properties).to().file(file(pom)).run();
  }

  protected void createProject(final File dir,
                               final String repository,
                               final String groupId,
                               final String artifactId,
                               final String version,
                               final String sourceRepository,
                               final String targetRepository)
  {
    final File pom = new File(dir, "pom.xml");
    final File rawPom = new File(dir, "raw-pom-with-move.xml");

    final Properties properties = getDefaultProperties(repository, groupId, artifactId, version, JAR_PACKAGING);
    properties.setProperty("nexus.destinationRepository", targetRepository);
    properties.setProperty("nexus.sourceRepository", sourceRepository);

    fileTaskBuilder.copy().file(file(rawPom)).filterUsing(properties).to().file(file(pom)).run();
  }

  private Properties getDefaultProperties(final String repository,
                                          final String groupId,
                                          final String artifactId,
                                          final String version,
                                          final String packaging)
  {
    final Properties properties = new Properties();
    properties.setProperty("nexus.url", "http://localhost:" + getPort());
    properties.setProperty("nexus.repository", repository);
    properties.setProperty("test.project.groupId", groupId);
    properties.setProperty("test.project.artifactId", artifactId);
    properties.setProperty("test.project.version", version);
    properties.setProperty("test.project.packaging", packaging);
    return properties;
  }

  protected void initialiseVerifier(final File projectRoot) throws Exception {
    String settingsXml = new File(projectRoot, "preset-nexus-maven-settings.xml").getAbsolutePath();

    verifier = new Verifier(projectRoot.getAbsolutePath(), settingsXml);

    verifier.setMavenDebug(true);

    // Cleanup the artifact in case it was previously built
    verifier.deleteArtifact(GROUP_ID, ARTIFACT_ID, VERSION, POM_PACKAGING);

    List<String> options = new ArrayList<>();
    options.add("-Djava.awt.headless=true"); // on Mac a Dock icon bumps on ever Verifier invocation
    options.add("-s " + settingsXml);
    verifier.setCliOptions(options);
  }
}
